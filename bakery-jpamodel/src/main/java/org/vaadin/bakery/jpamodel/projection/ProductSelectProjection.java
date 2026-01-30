package org.vaadin.bakery.jpamodel.projection;

import java.math.BigDecimal;

/**
 * Projection for order form product dropdown.
 */
public interface ProductSelectProjection {

    Long getId();

    String getName();

    String getSize();

    BigDecimal getPrice();
}
