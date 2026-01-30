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

    List<OrderList> listUpcoming();

    List<OrderList> listByDateRange(LocalDate startDate, LocalDate endDate);

    List<OrderList> listByStatus(OrderStatus status);

    List<OrderList> listByCustomer(Long customerId);

    Optional<OrderDetail> get(Long id);

    OrderDetail create(OrderDetail order);

    OrderDetail update(Long id, OrderDetail order);

    void updateStatus(Long id, OrderStatus newStatus);

    void markAsPaid(Long id);

    long countByStatus(OrderStatus status);

    long countByDate(LocalDate date);

    long countByDateExcludingStatuses(LocalDate date, List<OrderStatus> excludedStatuses);
}
