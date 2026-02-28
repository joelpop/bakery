package org.vaadin.bakery.jpamodel.projection;

import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;

import java.math.BigDecimal;

/**
 * Projection for order item display.
 */
public interface OrderItemSummaryProjection {

    Long getId();

    OrderItemStatusCode getStatus();

    Integer getQuantity();

    String getDetails();

    BigDecimal getUnitPrice();

    BigDecimal getLineTotal();

    String getProductName();

    String getProductSize();
}
