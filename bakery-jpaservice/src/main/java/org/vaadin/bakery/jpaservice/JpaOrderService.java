package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode;
import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;
import org.vaadin.bakery.jpamodel.code.OrderStatusCode;
import org.vaadin.bakery.jpamodel.entity.CustomerEntity;
import org.vaadin.bakery.jpamodel.entity.OrderActivityEntity;
import org.vaadin.bakery.jpamodel.entity.OrderEntity;
import org.vaadin.bakery.jpamodel.entity.OrderItemEntity;
import org.vaadin.bakery.jpaclient.repository.CustomerRepository;
import org.vaadin.bakery.jpaclient.repository.LocationRepository;
import org.vaadin.bakery.jpaclient.repository.OrderActivityRepository;
import org.vaadin.bakery.jpaclient.repository.OrderItemRepository;
import org.vaadin.bakery.jpaclient.repository.OrderRepository;
import org.vaadin.bakery.jpaclient.repository.ProductRepository;
import org.vaadin.bakery.jpaclient.repository.TileUndoEntryRepository;
import org.vaadin.bakery.jpaservice.mapper.EnumMapper;
import org.vaadin.bakery.jpaservice.mapper.OrderMapper;
import org.vaadin.bakery.service.DataChangeNotifier;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.uimodel.data.OrderDetail;
import org.vaadin.bakery.uimodel.data.OrderList;
import org.vaadin.bakery.uimodel.type.OrderItemStatus;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA implementation of the order service.
 */
@Service
@Transactional
public class JpaOrderService implements OrderService {

    private static final List<OrderStatusCode> TERMINAL_STATUSES = List.of(
            OrderStatusCode.PICKED_UP,
            OrderStatusCode.CANCELED
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderActivityRepository orderActivityRepository;
    private final CustomerRepository customerRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final EnumMapper enumMapper;
    private final DataChangeNotifier dataChangeNotifier;
    private final TileUndoEntryRepository tileUndoEntryRepository;

    /** Creates the order service with injected dependencies. */
    public JpaOrderService(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           OrderActivityRepository orderActivityRepository,
                           CustomerRepository customerRepository,
                           LocationRepository locationRepository,
                           ProductRepository productRepository,
                           OrderMapper orderMapper, EnumMapper enumMapper,
                           DataChangeNotifier dataChangeNotifier,
                           TileUndoEntryRepository tileUndoEntryRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderActivityRepository = orderActivityRepository;
        this.customerRepository = customerRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
        this.enumMapper = enumMapper;
        this.dataChangeNotifier = dataChangeNotifier;
        this.tileUndoEntryRepository = tileUndoEntryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderList> listUpcoming() {
        var orders = orderRepository.findUpcomingOrdersWithDetails(LocalDate.now());
        return orderMapper.toListList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderList> listByDateRange(LocalDate startDate, LocalDate endDate) {
        var orders = orderRepository.findByDueDateBetweenOrderByDueDateAscDueTimeAsc(startDate, endDate);
        return orderMapper.toListList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderList> listByStatus(OrderStatus status) {
        var statusCode = enumMapper.toOrderStatusCode(status);
        var orders = orderRepository.findByStatus(statusCode);
        return orderMapper.toListList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderList> listByCustomer(Long customerId) {
        var orders = orderRepository.findByCustomerIdOrderByDueDateDescDueTimeDesc(customerId);
        return orderMapper.toListList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDetail> get(Long id) {
        return orderRepository.findById(id).map(orderMapper::toDetail);
    }

    @Override
    public OrderDetail create(OrderDetail order) {
        var entity = orderMapper.toNewEntity(order);

        // Find or create customer
        CustomerEntity customer;
        var newCustomerCreated = false;
        if (order.getCustomerId() != null) {
            customer = customerRepository.findById(order.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + order.getCustomerId()));
        } else if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
            // Try to find existing customer by phone, or create new one
            var existingCustomer = customerRepository.findByPhoneNumber(order.getCustomerPhone());
            if (existingCustomer.isPresent()) {
                customer = existingCustomer.get();
            } else {
                var newCustomer = new CustomerEntity();
                newCustomer.setName(order.getCustomerName());
                newCustomer.setPhoneNumber(order.getCustomerPhone());
                newCustomer.setActive(true);
                customer = customerRepository.save(newCustomer);
                newCustomerCreated = true;
            }
        } else {
            throw new IllegalArgumentException("Either customerId or customerPhone must be provided");
        }
        entity.setCustomer(customer);

        var location = locationRepository.findById(order.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + order.getLocationId()));
        entity.setLocation(location);

        for (var itemDetail : order.getItems()) {
            var product = productRepository.findById(itemDetail.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDetail.getProductId()));

            var itemEntity = new OrderItemEntity();
            itemEntity.setQuantity(itemDetail.getQuantity());
            itemEntity.setDetails(itemDetail.getDetails());
            itemEntity.setUnitPrice(itemDetail.getUnitPrice());
            itemEntity.setLineTotal(itemDetail.getLineTotal());
            itemEntity.setProduct(product);
            entity.addItem(itemEntity);
        }

        var saved = orderRepository.save(entity);
        createSystemEvent(saved, "Order created");
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, saved.getId());
        var result = orderMapper.toDetail(saved);
        result.setNewCustomerCreated(newCustomerCreated);
        return result;
    }

    @Override
    public OrderDetail update(Long id, OrderDetail order) {
        var entity = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        // Capture current values before mapper overwrites them
        var oldDueDate = entity.getDueDate();
        var oldDueTime = entity.getDueTime();
        var oldLocationName = entity.getLocation().getName();
        var oldLocationId = entity.getLocation().getId();
        var oldAdditionalDetails = entity.getAdditionalDetails();

        orderMapper.toEntity(order, entity);

        if (!entity.getCustomer().getId().equals(order.getCustomerId())) {
            var customer = customerRepository.findById(order.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + order.getCustomerId()));
            entity.setCustomer(customer);
        }

        if (!entity.getLocation().getId().equals(order.getLocationId())) {
            var location = locationRepository.findById(order.getLocationId())
                    .orElseThrow(() -> new IllegalArgumentException("Location not found: " + order.getLocationId()));
            entity.setLocation(location);
        }

        // Sync items: clear existing, re-add from UI model (orphanRemoval handles deletes)
        entity.getItems().clear();
        for (var itemDetail : order.getItems()) {
            var product = productRepository.findById(itemDetail.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDetail.getProductId()));
            var itemEntity = new OrderItemEntity();
            itemEntity.setQuantity(itemDetail.getQuantity());
            itemEntity.setDetails(itemDetail.getDetails());
            itemEntity.setUnitPrice(itemDetail.getUnitPrice());
            itemEntity.setLineTotal(itemDetail.getLineTotal());
            itemEntity.setProduct(product);
            entity.addItem(itemEntity);
        }

        JpaServiceHelper.flushOrThrowStale(orderRepository, "order", id);

        // New items start as PENDING_REVIEW; recalculate order status from items
        recalculateOrderStatus(entity);

        // Emit system events for changed fields
        if (!entity.getDueDate().equals(oldDueDate) || !entity.getDueTime().equals(oldDueTime)) {
            createSystemEvent(entity, "Due date/time changed from " + oldDueDate + " " + oldDueTime
                    + " to " + entity.getDueDate() + " " + entity.getDueTime());
        }
        if (!entity.getLocation().getId().equals(oldLocationId)) {
            createSystemEvent(entity, "Location changed from " + oldLocationName
                    + " to " + entity.getLocation().getName());
        }
        var newDetails = entity.getAdditionalDetails();
        if (oldAdditionalDetails == null ? newDetails != null : !oldAdditionalDetails.equals(newDetails)) {
            createSystemEvent(entity, "Additional details updated");
        }

        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, id);
        return orderMapper.toDetail(entity);
    }

    @Override
    public void updateStatus(Long id, OrderStatus newStatus, Integer expectedVersion) {
        var entity = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        entity.setVersion(expectedVersion);
        var statusCode = enumMapper.toOrderStatusCode(newStatus);
        entity.setStatus(statusCode);
        JpaServiceHelper.flushOrThrowStale(orderRepository, "order", id);
        createSystemEvent(entity, "Status changed to " + newStatus.getDisplayName());

        // When canceling an order, clear undo entries for all its items
        if (statusCode == OrderStatusCode.CANCELED) {
            clearUndoEntriesForOrder(entity);
        }

        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, id);
    }

    @Override
    public void markAsPaid(Long id, Integer expectedVersion) {
        var entity = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        entity.setVersion(expectedVersion);
        entity.setPaid(true);
        JpaServiceHelper.flushOrThrowStale(orderRepository, "order", id);
        createSystemEvent(entity, "Marked as paid");
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> getVersion(Long id) {
        return orderRepository.findById(id).map(e -> e.getVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(enumMapper.toOrderStatusCode(status));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDate(LocalDate date) {
        return orderRepository.countByDueDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDateExcludingStatuses(LocalDate date, List<OrderStatus> excludedStatuses) {
        var excludedCodes = excludedStatuses.stream()
                .map(enumMapper::toOrderStatusCode)
                .toList();
        return orderRepository.countByDueDateAndStatusNotIn(date, excludedCodes);
    }

    @Override
    public void updateItemStatus(Long orderId, Long itemId, OrderItemStatus newStatus, Integer expectedItemVersion) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        var item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        var newStatusCode = enumMapper.toOrderItemStatusCode(newStatus);

        // Hold enforcement: ACCEPTED → IN_PROGRESS is blocked if on hold
        if (newStatusCode == OrderItemStatusCode.IN_PROGRESS
                && OrderStatusRollUpHelper.isOnHold(item, order.getItems())) {
            throw new IllegalStateException("Item is on hold — resolve or cancel sibling items first");
        }

        // Today-only rule: cannot start production for future-dated items
        if (newStatusCode == OrderItemStatusCode.IN_PROGRESS
                && order.getDueDate() != null
                && order.getDueDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Cannot start production for future-dated items");
        }

        item.setVersion(expectedItemVersion);
        item.setStatus(newStatusCode);
        JpaServiceHelper.flushOrThrowStale(orderItemRepository, "order item", itemId);

        createSystemEvent(order, "Item \"" + item.getProduct().getName() + "\" status changed to " + newStatus.getDisplayName());
        recalculateOrderStatus(order);
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, orderId);
    }

    @Override
    public void rejectItem(Long orderId, Long itemId, String message, Integer expectedItemVersion) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        var item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        item.setVersion(expectedItemVersion);
        item.setStatus(OrderItemStatusCode.REJECTED);
        JpaServiceHelper.flushOrThrowStale(orderItemRepository, "order item", itemId);

        createStaffMessage(order, message, item);
        createSystemEvent(order, "Item \"" + item.getProduct().getName() + "\" rejected");
        recalculateOrderStatus(order);
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, orderId);
    }

    @Override
    public void resolveItem(Long orderId, Long itemId, String message, Integer expectedItemVersion) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        var item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        if (item.getStatus() != OrderItemStatusCode.REJECTED) {
            throw new IllegalStateException("Only rejected items can be resolved");
        }

        item.setVersion(expectedItemVersion);
        item.setStatus(OrderItemStatusCode.PENDING_REVIEW);
        JpaServiceHelper.flushOrThrowStale(orderItemRepository, "order item", itemId);

        createStaffMessage(order, message, item);
        createSystemEvent(order, "Item \"" + item.getProduct().getName() + "\" resolved — returned to review");
        recalculateOrderStatus(order);
        clearUndoEntriesForItem(item);
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, orderId);
    }

    @Override
    public void cancelItem(Long orderId, Long itemId, String message, Integer expectedItemVersion) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        var item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        if (item.getStatus() != OrderItemStatusCode.REJECTED) {
            throw new IllegalStateException("Only rejected items can be canceled");
        }

        item.setVersion(expectedItemVersion);
        item.setStatus(OrderItemStatusCode.CANCELED);
        JpaServiceHelper.flushOrThrowStale(orderItemRepository, "order item", itemId);

        createStaffMessage(order, message, item);
        createSystemEvent(order, "Item \"" + item.getProduct().getName() + "\" canceled");
        recalculateOrderStatus(order);
        clearUndoEntriesForItem(item);
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, orderId);
    }

    @Override
    public void togglePaid(Long id, Integer expectedVersion) {
        var entity = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        entity.setVersion(expectedVersion);
        entity.setPaid(!entity.isPaid());
        JpaServiceHelper.flushOrThrowStale(orderRepository, "order", id);
        createSystemEvent(entity, entity.isPaid() ? "Marked as paid" : "Marked as unpaid");
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, id);
    }

    @Override
    public void togglePickedUp(Long id, Integer expectedVersion) {
        var entity = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        entity.setVersion(expectedVersion);

        if (entity.getStatus() == OrderStatusCode.READY_FOR_PICK_UP) {
            entity.setStatus(OrderStatusCode.PICKED_UP);
            JpaServiceHelper.flushOrThrowStale(orderRepository, "order", id);
            createSystemEvent(entity, "Status changed to Picked Up");
        } else if (entity.getStatus() == OrderStatusCode.PICKED_UP) {
            entity.setStatus(OrderStatusCode.READY_FOR_PICK_UP);
            JpaServiceHelper.flushOrThrowStale(orderRepository, "order", id);
            createSystemEvent(entity, "Status changed to Ready for Pick Up");
        } else {
            throw new IllegalStateException("Order must be Ready for Pick Up or Picked Up to toggle");
        }

        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER, id);
    }

    /**
     * Recalculates order status from item statuses using the roll-up helper.
     * If the status changed, persists and records a system event.
     */
    private void recalculateOrderStatus(OrderEntity order) {
        var derivedStatus = OrderStatusRollUpHelper.deriveOrderStatus(order.getItems());
        if (order.getStatus() != derivedStatus) {
            var oldStatus = order.getStatus();
            order.setStatus(derivedStatus);
            JpaServiceHelper.flushOrThrowStale(orderRepository, "order", order.getId());
            createSystemEvent(order, "Status changed from " + oldStatus + " to " + derivedStatus);
        }
    }

    /**
     * Clears all undo entries for grouping keys that include the given item.
     * Covers both batchable keys (batch:{productId}:{dueDate}:{status}) and
     * non-batchable keys (item:{itemId}).
     */
    private void clearUndoEntriesForItem(OrderItemEntity item) {
        // Clear non-batchable key
        tileUndoEntryRepository.deleteByGroupingKey(
                JpaBakeryService.buildNonBatchGroupingKey(item.getId()));
        // Clear batchable key for the item's current status
        tileUndoEntryRepository.deleteByGroupingKey(
                JpaBakeryService.buildBatchGroupingKey(
                        item.getProduct().getId(),
                        item.getOrder().getDueDate(),
                        item.getStatus()));
    }

    /**
     * Clears all undo entries for every item in the given order.
     */
    private void clearUndoEntriesForOrder(OrderEntity order) {
        var clearedKeys = new java.util.HashSet<String>();
        for (var item : order.getItems()) {
            var nonBatchKey = JpaBakeryService.buildNonBatchGroupingKey(item.getId());
            if (clearedKeys.add(nonBatchKey)) {
                tileUndoEntryRepository.deleteByGroupingKey(nonBatchKey);
            }
            var batchKey = JpaBakeryService.buildBatchGroupingKey(
                    item.getProduct().getId(),
                    order.getDueDate(),
                    item.getStatus());
            if (clearedKeys.add(batchKey)) {
                tileUndoEntryRepository.deleteByGroupingKey(batchKey);
            }
        }
    }

    private void createStaffMessage(OrderEntity order, String text, OrderItemEntity referencedItem) {
        var event = new OrderActivityEntity();
        event.setOrder(order);
        event.setType(OrderActivityTypeCode.STAFF_MESSAGE);
        event.setText(text);
        event.setReferencedItem(referencedItem);
        event.setPostedAt(Instant.now());
        event.setRead(false);
        orderActivityRepository.save(event);
    }

    private void createSystemEvent(OrderEntity order, String text) {
        var event = new OrderActivityEntity();
        event.setOrder(order);
        event.setType(OrderActivityTypeCode.SYSTEM_EVENT);
        event.setText(text);
        event.setPostedAt(Instant.now());
        event.setRead(true);
        orderActivityRepository.save(event);
    }
}
