package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;
import org.vaadin.bakery.jpamodel.entity.OrderActivityEntity;
import org.vaadin.bakery.jpamodel.entity.OrderItemEntity;
import org.vaadin.bakery.jpaclient.repository.OrderActivityRepository;
import org.vaadin.bakery.jpaclient.repository.OrderItemRepository;
import org.vaadin.bakery.jpaclient.repository.TilePositionRepository;
import org.vaadin.bakery.jpaclient.repository.TileUndoEntryRepository;
import org.vaadin.bakery.jpaservice.mapper.EnumMapper;
import org.vaadin.bakery.service.BakeryService;
import org.vaadin.bakery.service.DataChangeNotifier;
import org.vaadin.bakery.uimodel.data.BakeryTile;
import org.vaadin.bakery.uimodel.data.BakeryTileDetail;
import org.vaadin.bakery.uimodel.type.OrderItemStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @Transactional(readOnly = true)
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
        var existing = tilePositionRepository.findBySwimlaneAndDueDateAndGroupingKey(
                swimlaneCode, dueDate, groupingKey);

        var entity = existing.orElseGet(() -> {
            var newEntity = new org.vaadin.bakery.jpamodel.entity.TilePositionEntity();
            newEntity.setGroupingKey(groupingKey);
            newEntity.setSwimlane(swimlaneCode);
            newEntity.setDueDate(dueDate);
            return newEntity;
        });
        entity.setPosition(position);
        tilePositionRepository.save(entity);
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

        // Revert all items in the group back to previous status
        var items = findItemsByGroupingKey(groupingKey);
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

        // Clean up old tile positions for this grouping key
        tilePositionRepository.deleteByGroupingKey(groupingKey);

        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER);
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

    private void applyPositions(List<BakeryTile> tiles) {
        // Build a lookup of persisted positions by grouping key
        var allPositions = tilePositionRepository.findAll();
        Map<String, Integer> positionMap = new HashMap<>();
        for (var pos : allPositions) {
            positionMap.put(pos.getGroupingKey(), pos.getPosition());
        }

        // Apply positions: tiles with persisted positions get their value, others get MAX_VALUE
        for (var tile : tiles) {
            var pos = positionMap.get(tile.getGroupingKey());
            tile.setPosition(pos != null ? pos : Integer.MAX_VALUE);
        }

        // Sort by: status, dueDate, position, productName
        tiles.sort(Comparator
                .comparing((BakeryTile t) -> t.getStatus().ordinal())
                .thenComparing(BakeryTile::getDueDate)
                .thenComparing(BakeryTile::getPosition)
                .thenComparing(BakeryTile::getProductName));
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
}
