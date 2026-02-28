package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode;
import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;
import org.vaadin.bakery.jpamodel.entity.OrderActivityEntity;
import org.vaadin.bakery.jpamodel.entity.OrderEntity;
import org.vaadin.bakery.jpamodel.entity.OrderItemEntity;
import org.vaadin.bakery.jpamodel.entity.TilePositionEntity;
import org.vaadin.bakery.jpamodel.entity.TileUndoEntryEntity;
import org.vaadin.bakery.jpaclient.repository.OrderActivityRepository;
import org.vaadin.bakery.jpaclient.repository.OrderItemRepository;
import org.vaadin.bakery.jpaclient.repository.TilePositionRepository;
import org.vaadin.bakery.jpaclient.repository.TileUndoEntryRepository;
import org.vaadin.bakery.jpaservice.mapper.EnumMapper;
import org.vaadin.bakery.service.BakeryService;
import org.vaadin.bakery.service.DataChangeNotifier;
import org.vaadin.bakery.service.StaleDataException;
import org.vaadin.bakery.uimodel.data.BakeryTile;
import org.vaadin.bakery.uimodel.data.BakeryTileDetail;
import org.vaadin.bakery.uimodel.type.OrderItemStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JPA implementation of the bakery board service.
 *
 * <p>Provides tile listing with batchable grouping, position persistence,
 * and undo support for bakery board transitions.
 */
@Service
@Transactional
public class JpaBakeryService implements BakeryService {

    private static final List<OrderItemStatusCode> TERMINAL_ITEM_STATUSES = List.of(
            OrderItemStatusCode.PRODUCED,
            OrderItemStatusCode.CANCELED
    );

    private final OrderItemRepository orderItemRepository;
    private final OrderActivityRepository orderActivityRepository;
    private final TilePositionRepository tilePositionRepository;
    private final TileUndoEntryRepository tileUndoEntryRepository;
    private final EnumMapper enumMapper;
    private final DataChangeNotifier dataChangeNotifier;

    /** Creates the bakery service with injected dependencies. */
    public JpaBakeryService(OrderItemRepository orderItemRepository,
                            OrderActivityRepository orderActivityRepository,
                            TilePositionRepository tilePositionRepository,
                            TileUndoEntryRepository tileUndoEntryRepository,
                            EnumMapper enumMapper,
                            DataChangeNotifier dataChangeNotifier) {
        this.orderItemRepository = orderItemRepository;
        this.orderActivityRepository = orderActivityRepository;
        this.tilePositionRepository = tilePositionRepository;
        this.tileUndoEntryRepository = tileUndoEntryRepository;
        this.enumMapper = enumMapper;
        this.dataChangeNotifier = dataChangeNotifier;
    }

    @Override
    public List<BakeryTile> listTiles(LocalDate startDate, LocalDate endDate) {
        var items = orderItemRepository.findItemsForBakeryBoard(startDate, endDate, TERMINAL_ITEM_STATUSES);

        // Collect all order IDs for unread message detection
        var orderIds = items.stream()
                .map(i -> i.getOrder().getId())
                .distinct()
                .toList();
        var unreadOrderIds = orderIds.isEmpty()
                ? java.util.Collections.<Long>emptySet()
                : orderActivityRepository.findOrderIdsWithUnreadMessages(orderIds);

        // Group items: batchable by (productId, dueDate, status), non-batchable individually
        var tiles = new ArrayList<BakeryTile>();
        var batchGroups = new HashMap<String, List<OrderItemEntity>>();

        for (var item : items) {
            var product = item.getProduct();
            if (product.isBatchable()) {
                var key = buildBatchGroupingKey(product.getId(), item.getOrder().getDueDate(), item.getStatus());
                batchGroups.computeIfAbsent(key, _ -> new ArrayList<>()).add(item);
            } else {
                tiles.add(buildNonBatchableTile(item, unreadOrderIds));
            }
        }

        // Build tiles for each batch group
        for (var entry : batchGroups.entrySet()) {
            tiles.add(buildBatchableTile(entry.getKey(), entry.getValue(), unreadOrderIds));
        }

        // Look up persisted positions and apply ordering
        applyPositions(tiles);

        // Check undo availability
        var undoKeys = tileUndoEntryRepository.findAll().stream()
                .map(e -> e.getGroupingKey())
                .collect(Collectors.toSet());
        for (var tile : tiles) {
            tile.setUndoAvailable(undoKeys.contains(tile.getGroupingKey()));
        }

        return tiles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BakeryTileDetail> getTileDetails(String groupingKey) {
        // Parse grouping key to find matching items
        var items = findItemsByGroupingKey(groupingKey);

        // Collect order IDs for unread check
        var orderIds = items.stream()
                .map(i -> i.getOrder().getId())
                .distinct()
                .toList();
        var unreadOrderIds = orderIds.isEmpty()
                ? java.util.Collections.<Long>emptySet()
                : orderActivityRepository.findOrderIdsWithUnreadMessages(orderIds);

        return items.stream()
                .map(item -> {
                    var detail = new BakeryTileDetail();
                    detail.setOrderId(item.getOrder().getId());
                    detail.setItemId(item.getId());
                    detail.setItemVersion(item.getVersion());
                    detail.setCustomerName(item.getOrder().getCustomer() != null
                            ? item.getOrder().getCustomer().getName() : "Unknown");
                    detail.setQuantity(item.getQuantity());
                    detail.setItemDetails(item.getDetails());
                    detail.setAdditionalDetails(item.getOrder().getAdditionalDetails());
                    detail.setHasUnreadMessages(unreadOrderIds.contains(item.getOrder().getId()));
                    return detail;
                })
                .toList();
    }

    @Override
    public void saveTilePosition(String groupingKey, OrderItemStatus swimlane, LocalDate dueDate, int position) {
        var swimlaneCode = enumMapper.toOrderItemStatusCode(swimlane);

        // For batch tiles, the grouping key includes the status ("batch:{productId}:{dueDate}:{status}").
        // After a status transition the caller still has the old key, so update the status segment
        // to match the target swimlane. This ensures the position record matches the key that
        // listTiles() will generate for the transitioned items.
        var resolvedKey = resolveBatchGroupingKey(groupingKey, swimlaneCode);

        var existing = tilePositionRepository.findBySwimlaneAndDueDateAndGroupingKey(
                swimlaneCode, dueDate, resolvedKey);

        var entity = existing.orElseGet(() -> {
            var newEntity = new TilePositionEntity();
            newEntity.setGroupingKey(resolvedKey);
            newEntity.setSwimlane(swimlaneCode);
            newEntity.setDueDate(dueDate);
            return newEntity;
        });
        entity.setPosition(position);
        tilePositionRepository.save(entity);
    }

    /**
     * For batch grouping keys ({@code "batch:{productId}:{dueDate}:{status}"}), replaces the
     * status segment with the target swimlane status. Non-batch keys are returned unchanged.
     */
    private static String resolveBatchGroupingKey(String groupingKey, OrderItemStatusCode swimlane) {
        if (groupingKey.startsWith("batch:")) {
            var lastColon = groupingKey.lastIndexOf(':');
            return groupingKey.substring(0, lastColon + 1) + swimlane.name();
        }
        return groupingKey;
    }

    @Override
    public void saveTileOrder(OrderItemStatus swimlane, LocalDate dueDate,
                              List<String> orderedGroupingKeys, String movedGroupingKey) {
        for (var i = 0; i < orderedGroupingKeys.size(); i++) {
            saveTilePosition(orderedGroupingKeys.get(i), swimlane, dueDate, i);
        }
        dataChangeNotifier.notifyTileChange(movedGroupingKey);
    }

    @Override
    public void transitionTilePosition(String groupingKey, OrderItemStatus fromStatus,
                                        OrderItemStatus toStatus, LocalDate dueDate, int position) {
        var fromCode = enumMapper.toOrderItemStatusCode(fromStatus);
        var toCode = enumMapper.toOrderItemStatusCode(toStatus);
        var oldKey = resolveBatchGroupingKey(groupingKey, fromCode);
        var newKey = resolveBatchGroupingKey(groupingKey, toCode);

        // Remove from source group
        tilePositionRepository.findBySwimlaneAndDueDateAndGroupingKey(fromCode, dueDate, oldKey)
                .ifPresent(tilePositionRepository::delete);
        tilePositionRepository.flush();

        // Load target group records in position order
        var targetRecords = new ArrayList<>(
                tilePositionRepository.findBySwimlaneAndDueDateOrderByPositionAsc(toCode, dueDate));

        // Reuse existing record if present (self-healing may have created one already),
        // otherwise create a new one. This avoids unique constraint violations on
        // (swimlane, dueDate, groupingKey).
        var existingIdx = -1;
        for (var i = 0; i < targetRecords.size(); i++) {
            if (targetRecords.get(i).getGroupingKey().equals(newKey)) {
                existingIdx = i;
                break;
            }
        }

        TilePositionEntity record;
        if (existingIdx >= 0) {
            record = targetRecords.remove(existingIdx);
        } else {
            record = new TilePositionEntity();
            record.setGroupingKey(newKey);
            record.setSwimlane(toCode);
            record.setDueDate(dueDate);
        }

        // Clamp position to valid range and insert
        var insertAt = Math.min(position, targetRecords.size());
        targetRecords.add(insertAt, record);

        // Resequence target group: 0, 1, 2, ...
        for (var i = 0; i < targetRecords.size(); i++) {
            targetRecords.get(i).setPosition(i);
        }
        tilePositionRepository.saveAll(targetRecords);

        // Resequence source group to close the gap
        var sourceRecords = tilePositionRepository
                .findBySwimlaneAndDueDateOrderByPositionAsc(fromCode, dueDate);
        for (var i = 0; i < sourceRecords.size(); i++) {
            sourceRecords.get(i).setPosition(i);
        }
        if (!sourceRecords.isEmpty()) {
            tilePositionRepository.saveAll(sourceRecords);
        }
    }

    @Override
    public void transitionTile(BakeryTile tile, OrderItemStatus newStatus,
                               int position, String rejectionMessage) {
        var items = findItemsByGroupingKey(tile.getGroupingKey());
        if (items.isEmpty()) {
            throw new IllegalStateException("No items found for tile: " + tile.getGroupingKey());
        }

        var newStatusCode = enumMapper.toOrderItemStatusCode(newStatus);
        var previousStatusCode = items.getFirst().getStatus();
        var dueDate = items.getFirst().getOrder().getDueDate();

        // Validation: hold enforcement
        if (newStatusCode == OrderItemStatusCode.IN_PROGRESS) {
            for (var item : items) {
                if (OrderStatusRollUpHelper.isOnHold(item, item.getOrder().getItems())) {
                    throw new IllegalStateException("Item is on hold — resolve or cancel sibling items first");
                }
            }
        }

        // Validation: today-only rule for IN_PROGRESS
        if (newStatusCode == OrderItemStatusCode.IN_PROGRESS
                && dueDate != null && dueDate.isAfter(LocalDate.now())) {
            throw new IllegalStateException("Cannot start production for future-dated items");
        }

        // Version check: set expected versions from the tile
        var expectedVersions = new HashMap<Long, Integer>();
        if (tile.isBatchable()) {
            for (var i = 0; i < tile.getItemIds().size(); i++) {
                expectedVersions.put(tile.getItemIds().get(i), tile.getItemVersions().get(i));
            }
        } else {
            expectedVersions.put(tile.getItemId(), tile.getItemVersion());
        }
        for (var item : items) {
            var expectedVersion = expectedVersions.get(item.getId());
            if (expectedVersion != null) {
                item.setVersion(expectedVersion);
            }
        }

        // Position management
        transitionTilePosition(tile.getGroupingKey(), tile.getStatus(), newStatus, dueDate, position);

        // Status update
        for (var item : items) {
            item.setStatus(newStatusCode);
        }
        JpaServiceHelper.flushOrThrowStale(orderItemRepository, "order item", items.getFirst().getId());

        // System events and rejection messages
        var activityIds = new ArrayList<Long>();
        for (var item : items) {
            var order = item.getOrder();
            var event = createSystemEvent(order,
                    "Item \"" + item.getProduct().getName() + "\" status changed to "
                            + newStatus.getDisplayName());
            activityIds.add(event.getId());

            if (newStatusCode == OrderItemStatusCode.REJECTED && rejectionMessage != null) {
                var msg = createStaffMessage(order, rejectionMessage, item);
                activityIds.add(msg.getId());
            }
        }

        // Order status roll-up
        var affectedOrders = items.stream()
                .map(OrderItemEntity::getOrder)
                .distinct()
                .toList();
        for (var order : affectedOrders) {
            var derivedStatus = OrderStatusRollUpHelper.deriveOrderStatus(order.getItems());
            if (order.getStatus() != derivedStatus) {
                order.setStatus(derivedStatus);
            }
        }

        // Undo entry
        recordUndoEntry(tile.getGroupingKey(), previousStatusCode, activityIds);

        // Notifications (one per affected order + tile-level)
        for (var order : affectedOrders) {
            dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, order.getId());
        }
        var newKey = resolveBatchGroupingKey(tile.getGroupingKey(), newStatusCode);
        dataChangeNotifier.notifyTileChange(newKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemStatus> getUndoStack(String groupingKey) {
        return tileUndoEntryRepository.findByGroupingKeyOrderBySequenceNumberDesc(groupingKey)
                .stream()
                .map(e -> enumMapper.toOrderItemStatus(e.getPreviousStatus()))
                .toList();
    }

    @Override
    public OrderItemStatus undoTileTransition(String groupingKey) {
        var topEntry = tileUndoEntryRepository.findFirstByGroupingKeyOrderBySequenceNumberDesc(groupingKey)
                .orElseThrow(() -> new IllegalStateException("No undo entries for tile: " + groupingKey));

        var previousStatus = topEntry.getPreviousStatus();

        // Determine current status before reverting (needed for position management)
        var items = findItemsByGroupingKey(groupingKey);
        var currentStatusCode = items.isEmpty() ? null : items.getFirst().getStatus();
        var dueDate = items.isEmpty() ? null : items.getFirst().getOrder().getDueDate();

        // Revert all items in the group back to previous status
        for (var item : items) {
            item.setStatus(previousStatus);
        }

        // Delete associated activity timeline entries if stored
        if (topEntry.getActivityIds() != null && !topEntry.getActivityIds().isBlank()) {
            var activityIds = java.util.Arrays.stream(topEntry.getActivityIds().split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();
            orderActivityRepository.deleteAllById(activityIds);
        }

        // Delete the undo entry
        tileUndoEntryRepository.delete(topEntry);

        // Recalculate order statuses for affected orders
        var affectedOrders = items.stream()
                .map(OrderItemEntity::getOrder)
                .distinct()
                .toList();
        for (var order : affectedOrders) {
            var derivedStatus = OrderStatusRollUpHelper.deriveOrderStatus(order.getItems());
            if (order.getStatus() != derivedStatus) {
                order.setStatus(derivedStatus);
            }
        }

        // Surgical position management: move tile from current group to reverted group
        if (currentStatusCode != null && dueDate != null) {
            var currentKey = resolveBatchGroupingKey(groupingKey, currentStatusCode);

            // Remove from current group
            tilePositionRepository.findBySwimlaneAndDueDateAndGroupingKey(
                    currentStatusCode, dueDate, currentKey)
                    .ifPresent(tilePositionRepository::delete);
            tilePositionRepository.flush();

            // Resequence current (source) group to close the gap
            var sourceRecords = tilePositionRepository
                    .findBySwimlaneAndDueDateOrderByPositionAsc(currentStatusCode, dueDate);
            for (var i = 0; i < sourceRecords.size(); i++) {
                sourceRecords.get(i).setPosition(i);
            }
            if (!sourceRecords.isEmpty()) {
                tilePositionRepository.saveAll(sourceRecords);
            }

            // Append to reverted (target) group at end
            var targetKey = resolveBatchGroupingKey(groupingKey, previousStatus);
            var targetRecords = tilePositionRepository
                    .findBySwimlaneAndDueDateOrderByPositionAsc(previousStatus, dueDate);
            var newRecord = new TilePositionEntity();
            newRecord.setGroupingKey(targetKey);
            newRecord.setSwimlane(previousStatus);
            newRecord.setDueDate(dueDate);
            newRecord.setPosition(targetRecords.size());
            tilePositionRepository.save(newRecord);
        }

        for (var order : affectedOrders) {
            dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, order.getId());
        }
        var revertedKey = resolveBatchGroupingKey(groupingKey, previousStatus);
        dataChangeNotifier.notifyTileChange(revertedKey);
        return enumMapper.toOrderItemStatus(previousStatus);
    }

    /**
     * Builds a grouping key for batchable items: "batch:{productId}:{dueDate}:{status}".
     */
    static String buildBatchGroupingKey(Long productId, LocalDate dueDate, OrderItemStatusCode status) {
        return "batch:" + productId + ":" + dueDate + ":" + status;
    }

    /**
     * Builds a grouping key for non-batchable items: "item:{itemId}".
     */
    static String buildNonBatchGroupingKey(Long itemId) {
        return "item:" + itemId;
    }

    private BakeryTile buildNonBatchableTile(OrderItemEntity item, java.util.Set<Long> unreadOrderIds) {
        var tile = new BakeryTile();
        tile.setGroupingKey(buildNonBatchGroupingKey(item.getId()));
        tile.setBatchable(false);
        tile.setProductId(item.getProduct().getId());
        tile.setProductName(item.getProduct().getName());
        tile.setProductSize(item.getProduct().getSize());
        tile.setDueDate(item.getOrder().getDueDate());
        tile.setStatus(enumMapper.toOrderItemStatus(item.getStatus()));
        tile.setTotalQuantity(item.getQuantity());
        tile.setOrderCount(1);
        tile.setHasNotes(item.getDetails() != null && !item.getDetails().isBlank());
        tile.setHasUnreadMessages(unreadOrderIds.contains(item.getOrder().getId()));
        tile.setOnHold(OrderStatusRollUpHelper.isOnHold(item, item.getOrder().getItems()));

        // Non-batchable specific fields
        tile.setOrderId(item.getOrder().getId());
        tile.setItemId(item.getId());
        tile.setItemVersion(item.getVersion());
        tile.setOrderReference("#" + item.getOrder().getId());
        tile.setItemDetails(item.getDetails());

        return tile;
    }

    private BakeryTile buildBatchableTile(String groupingKey, List<OrderItemEntity> items,
                                          java.util.Set<Long> unreadOrderIds) {
        var first = items.getFirst();
        var tile = new BakeryTile();
        tile.setGroupingKey(groupingKey);
        tile.setBatchable(true);
        tile.setProductId(first.getProduct().getId());
        tile.setProductName(first.getProduct().getName());
        tile.setProductSize(first.getProduct().getSize());
        tile.setDueDate(first.getOrder().getDueDate());
        tile.setStatus(enumMapper.toOrderItemStatus(first.getStatus()));

        var totalQty = items.stream().mapToInt(OrderItemEntity::getQuantity).sum();
        tile.setTotalQuantity(totalQty);

        var distinctOrders = items.stream()
                .map(i -> i.getOrder().getId())
                .distinct()
                .count();
        tile.setOrderCount((int) distinctOrders);

        var hasNotes = items.stream()
                .anyMatch(i -> i.getDetails() != null && !i.getDetails().isBlank());
        tile.setHasNotes(hasNotes);

        var hasUnread = items.stream()
                .anyMatch(i -> unreadOrderIds.contains(i.getOrder().getId()));
        tile.setHasUnreadMessages(hasUnread);

        var anyOnHold = items.stream()
                .anyMatch(i -> OrderStatusRollUpHelper.isOnHold(i, i.getOrder().getItems()));
        tile.setOnHold(anyOnHold);

        // Batchable specific fields
        tile.setItemIds(items.stream().map(OrderItemEntity::getId).toList());
        tile.setItemVersions(items.stream().map(OrderItemEntity::getVersion).toList());

        return tile;
    }

    /**
     * Applies persisted positions to tiles, self-healing any inconsistencies.
     *
     * <p>Groups tiles by (status, dueDate) and verifies each group has sequential
     * positions (0, 1, 2, ...) with no gaps, orphans, or unpositioned tiles. If any
     * group is inconsistent, it is resequenced and persisted before positions are applied.
     */
    private void applyPositions(List<BakeryTile> tiles) {
        // Group tiles by (swimlane, dueDate) to know what should exist in each group
        var tilesByGroup = new HashMap<String, Set<String>>();
        for (var tile : tiles) {
            var swimlaneCode = enumMapper.toOrderItemStatusCode(tile.getStatus());
            var groupKey = swimlaneCode + "|" + tile.getDueDate();
            tilesByGroup.computeIfAbsent(groupKey, _ -> new LinkedHashSet<>())
                    .add(tile.getGroupingKey());
        }

        // Load all position records and group by (swimlane, dueDate)
        var allPositions = tilePositionRepository.findAll();
        var recordsByGroup = new HashMap<String, List<TilePositionEntity>>();
        for (var pos : allPositions) {
            var groupKey = pos.getSwimlane() + "|" + pos.getDueDate();
            recordsByGroup.computeIfAbsent(groupKey, _ -> new ArrayList<>()).add(pos);
        }

        // Detect and heal inconsistencies per group
        var dirtyGroups = new ArrayList<String>();
        for (var entry : tilesByGroup.entrySet()) {
            var groupKey = entry.getKey();
            var liveKeys = entry.getValue();
            var records = recordsByGroup.getOrDefault(groupKey, List.of());

            if (isGroupInconsistent(liveKeys, records)) {
                dirtyGroups.add(groupKey);
            }
        }

        // Also check for orphaned groups: position records for groups with no live tiles
        for (var groupKey : recordsByGroup.keySet()) {
            if (tilesByGroup.containsKey(groupKey)) {
                continue; // Already checked above
            }
            // Position records exist but no tiles — orphaned group, will be cleaned up
            dirtyGroups.add(groupKey);
        }

        if (!dirtyGroups.isEmpty()) {
            for (var groupKey : dirtyGroups) {
                var parts = groupKey.split("\\|", 2);
                var swimlaneCode = OrderItemStatusCode.valueOf(parts[0]);
                var dueDate = LocalDate.parse(parts[1]);
                var liveKeys = tilesByGroup.getOrDefault(groupKey, Set.of());
                resequenceGroup(swimlaneCode, dueDate, liveKeys);
            }

            // Reload positions after healing
            allPositions = tilePositionRepository.findAll();
        }

        // Build composite lookup and apply to tiles
        Map<String, Integer> positionMap = new HashMap<>();
        for (var pos : allPositions) {
            positionMap.put(pos.getSwimlane() + ":" + pos.getGroupingKey(), pos.getPosition());
        }

        for (var tile : tiles) {
            var swimlaneCode = enumMapper.toOrderItemStatusCode(tile.getStatus());
            var compositeKey = swimlaneCode + ":" + tile.getGroupingKey();
            var pos = positionMap.get(compositeKey);
            tile.setPosition(pos != null ? pos : 0);
        }

        // Sort by: status, dueDate, position, productName
        tiles.sort(Comparator
                .comparing((BakeryTile t) -> t.getStatus().ordinal())
                .thenComparing(BakeryTile::getDueDate)
                .thenComparing(BakeryTile::getPosition)
                .thenComparing(BakeryTile::getProductName));
    }

    /**
     * Checks whether a (swimlane, dueDate) group has inconsistent positions.
     *
     * <p>Returns {@code true} if there are unpositioned tiles, orphaned records,
     * or positions that are not sequential (0, 1, 2, ...).
     */
    private static boolean isGroupInconsistent(Set<String> liveKeys,
                                                List<TilePositionEntity> records) {
        // Check: all live keys have records and all records match live keys
        var recordKeys = records.stream()
                .map(TilePositionEntity::getGroupingKey)
                .collect(Collectors.toSet());
        if (!recordKeys.equals(liveKeys)) {
            return true;
        }

        // Check: positions are sequential 0..n-1
        var positions = records.stream()
                .map(TilePositionEntity::getPosition)
                .sorted()
                .toList();
        for (var i = 0; i < positions.size(); i++) {
            if (positions.get(i) != i) {
                return true;
            }
        }

        return false;
    }

    /**
     * Resequences all tile positions in a (swimlane, dueDate) group to be sequential.
     *
     * <p>Deletes orphaned records (tiles no longer in the group), creates records for
     * unpositioned tiles (appended at end sorted by grouping key), and reassigns
     * positions as 0, 1, 2, ...
     */
    private void resequenceGroup(OrderItemStatusCode swimlane, LocalDate dueDate,
                                  Set<String> liveTileKeys) {
        var records = tilePositionRepository
                .findBySwimlaneAndDueDateOrderByPositionAsc(swimlane, dueDate);

        // Separate live from orphaned
        var liveRecords = new ArrayList<TilePositionEntity>();
        var orphanedRecords = new ArrayList<TilePositionEntity>();
        for (var record : records) {
            if (liveTileKeys.contains(record.getGroupingKey())) {
                liveRecords.add(record);
            } else {
                orphanedRecords.add(record);
            }
        }

        // Delete orphaned records
        if (!orphanedRecords.isEmpty()) {
            tilePositionRepository.deleteAll(orphanedRecords);
        }

        // Create records for unpositioned tiles, appended at end
        var positionedKeys = liveRecords.stream()
                .map(TilePositionEntity::getGroupingKey)
                .collect(Collectors.toSet());
        var unpositionedKeys = liveTileKeys.stream()
                .filter(k -> !positionedKeys.contains(k))
                .sorted()
                .toList();
        for (var key : unpositionedKeys) {
            var entity = new TilePositionEntity();
            entity.setGroupingKey(key);
            entity.setSwimlane(swimlane);
            entity.setDueDate(dueDate);
            liveRecords.add(entity);
        }

        // Reassign sequential positions and save
        for (var i = 0; i < liveRecords.size(); i++) {
            liveRecords.get(i).setPosition(i);
        }
        if (!liveRecords.isEmpty()) {
            tilePositionRepository.saveAll(liveRecords);
        }
    }

    private List<OrderItemEntity> findItemsByGroupingKey(String groupingKey) {
        if (groupingKey.startsWith("item:")) {
            var itemId = Long.parseLong(groupingKey.substring("item:".length()));
            return orderItemRepository.findById(itemId)
                    .map(List::of)
                    .orElse(List.of());
        } else if (groupingKey.startsWith("batch:")) {
            // Parse "batch:{productId}:{dueDate}:{status}"
            var parts = groupingKey.split(":", 4);
            var productId = Long.parseLong(parts[1]);
            var dueDate = LocalDate.parse(parts[2]);
            var status = OrderItemStatusCode.valueOf(parts[3]);

            // Query all items matching these criteria
            return orderItemRepository.findItemsForBakeryBoard(dueDate, dueDate, TERMINAL_ITEM_STATUSES)
                    .stream()
                    .filter(i -> i.getProduct().getId().equals(productId)
                            && i.getOrder().getDueDate().equals(dueDate)
                            && i.getStatus() == status)
                    .toList();
        }
        return List.of();
    }

    /**
     * Creates a system event activity entry for the given order.
     *
     * @return the persisted activity entity (with generated ID)
     */
    private OrderActivityEntity createSystemEvent(OrderEntity order, String text) {
        var event = new OrderActivityEntity();
        event.setOrder(order);
        event.setType(OrderActivityTypeCode.SYSTEM_EVENT);
        event.setText(text);
        event.setPostedAt(Instant.now());
        event.setRead(true);
        return orderActivityRepository.save(event);
    }

    /**
     * Creates a staff message activity entry for a rejection.
     *
     * @return the persisted activity entity (with generated ID)
     */
    private OrderActivityEntity createStaffMessage(OrderEntity order, String text,
                                                    OrderItemEntity referencedItem) {
        var event = new OrderActivityEntity();
        event.setOrder(order);
        event.setType(OrderActivityTypeCode.STAFF_MESSAGE);
        event.setText(text);
        event.setReferencedItem(referencedItem);
        event.setPostedAt(Instant.now());
        event.setRead(false);
        return orderActivityRepository.save(event);
    }

    /**
     * Records an undo stack entry for a tile transition.
     */
    private void recordUndoEntry(String groupingKey, OrderItemStatusCode previousStatus,
                                  List<Long> activityIds) {
        var maxSeq = tileUndoEntryRepository
                .findFirstByGroupingKeyOrderBySequenceNumberDesc(groupingKey)
                .map(TileUndoEntryEntity::getSequenceNumber).orElse(-1);
        var entry = new TileUndoEntryEntity();
        entry.setGroupingKey(groupingKey);
        entry.setPreviousStatus(previousStatus);
        entry.setSequenceNumber(maxSeq + 1);
        entry.setActivityIds(activityIds.stream()
                .map(String::valueOf).collect(Collectors.joining(",")));
        tileUndoEntryRepository.save(entry);
    }
}
