package org.vaadin.bakery.jpamodel.projection;

import org.vaadin.bakery.jpamodel.code.OrderStatusCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Projection for storefront order list display (with items).
 */
public interface OrderListProjection {

    Long getId();

    OrderStatusCode getStatus();

    LocalDate getDueDate();

    LocalTime getDueTime();

    BigDecimal getTotal();

    boolean isPaid();

    LocalDateTime getCreatedAt();

    String getCustomerName();

    String getLocationName();

    List<OrderItemSummaryProjection> getItems();
}
