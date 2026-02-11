package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.OrderDetail;
import org.vaadin.bakery.uimodel.data.OrderList;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for order management operations.
 */
public interface OrderService {

    /**
     * Returns all upcoming orders (scheduled for today or later).
     */
    List<OrderList> listUpcoming();

    /**
     * Returns orders whose due date falls within the given date range (inclusive).
     */
    List<OrderList> listByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Returns orders with the given status.
     */
    List<OrderList> listByStatus(OrderStatus status);

    /**
     * Returns orders belonging to the given customer.
     */
    List<OrderList> listByCustomer(Long customerId);

    /**
     * Returns the full order detail for the given ID, if found.
     */
    Optional<OrderDetail> get(Long id);

    /**
     * Creates a new order and returns the saved result.
     */
    OrderDetail create(OrderDetail order);

    /**
     * Updates an existing order identified by the given ID.
     */
    OrderDetail update(Long id, OrderDetail order);

    /**
     * Transitions the order to a new status, verifying the expected version for optimistic locking.
     */
    void updateStatus(Long id, OrderStatus newStatus, Integer expectedVersion);

    /**
     * Marks the order as paid, verifying the expected version for optimistic locking.
     */
    void markAsPaid(Long id, Integer expectedVersion);

    /**
     * Returns the current optimistic-lock version for the given order.
     */
    Optional<Integer> getVersion(Long id);

    /**
     * Returns the number of orders with the given status.
     */
    long countByStatus(OrderStatus status);

    /**
     * Returns the number of orders due on the given date.
     */
    long countByDate(LocalDate date);

    /**
     * Returns the number of orders due on the given date, excluding those with any of the specified statuses.
     */
    long countByDateExcludingStatuses(LocalDate date, List<OrderStatus> excludedStatuses);
}
