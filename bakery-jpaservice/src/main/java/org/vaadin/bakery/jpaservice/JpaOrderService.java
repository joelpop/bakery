package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode;
import org.vaadin.bakery.jpamodel.code.OrderStatusCode;
import org.vaadin.bakery.jpamodel.entity.CustomerEntity;
import org.vaadin.bakery.jpamodel.entity.OrderActivityEntity;
import org.vaadin.bakery.jpamodel.entity.OrderEntity;
import org.vaadin.bakery.jpamodel.entity.OrderItemEntity;
import org.vaadin.bakery.jpaclient.repository.CustomerRepository;
import org.vaadin.bakery.jpaclient.repository.LocationRepository;
import org.vaadin.bakery.jpaclient.repository.OrderActivityRepository;
import org.vaadin.bakery.jpaclient.repository.OrderRepository;
import org.vaadin.bakery.jpaclient.repository.ProductRepository;
import org.vaadin.bakery.jpaservice.mapper.EnumMapper;
import org.vaadin.bakery.jpaservice.mapper.OrderMapper;
import org.vaadin.bakery.service.DataChangeNotifier;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.uimodel.data.OrderDetail;
import org.vaadin.bakery.uimodel.data.OrderList;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of the order service.
 */
@Service
@Transactional
public class JpaOrderService implements OrderService {

    private static final List<OrderStatusCode> TERMINAL_STATUSES = List.of(
            OrderStatusCode.PICKED_UP,
            OrderStatusCode.CANCELLED
    );

    private final OrderRepository orderRepository;
    private final OrderActivityRepository orderActivityRepository;
    private final CustomerRepository customerRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final EnumMapper enumMapper;
    private final DataChangeNotifier dataChangeNotifier;

    public JpaOrderService(OrderRepository orderRepository,
                           OrderActivityRepository orderActivityRepository,
                           CustomerRepository customerRepository,
                           LocationRepository locationRepository,
                           ProductRepository productRepository,
                           OrderMapper orderMapper, EnumMapper enumMapper,
                           DataChangeNotifier dataChangeNotifier) {
        this.orderRepository = orderRepository;
        this.orderActivityRepository = orderActivityRepository;
        this.customerRepository = customerRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
        this.enumMapper = enumMapper;
        this.dataChangeNotifier = dataChangeNotifier;
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
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER);
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

        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER);
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
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER);
    }

    @Override
    public void markAsPaid(Long id, Integer expectedVersion) {
        var entity = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        entity.setVersion(expectedVersion);
        entity.setPaid(true);
        JpaServiceHelper.flushOrThrowStale(orderRepository, "order", id);
        createSystemEvent(entity, "Marked as paid");
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.ORDER);
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
