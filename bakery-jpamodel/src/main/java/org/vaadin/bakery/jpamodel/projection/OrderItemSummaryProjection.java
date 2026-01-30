package org.vaadin.bakery.jpamodel.projection;

import java.math.BigDecimal;

/**
 * Projection for order item display.
 */
public interface OrderItemSummaryProjection {

    Long getId();

    Integer getQuantity();

    String getDetails();

    BigDecimal getUnitPrice();

    BigDecimal getLineTotal();

    String getProductName();

    String getProductSize();
}
