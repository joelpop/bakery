package org.vaadin.bakery.jpamodel.projection;

import java.math.BigDecimal;

/**
 * Projection for product admin grid display.
 */
public interface ProductSummaryProjection {

    Long getId();

    String getName();

    String getDescription();

    String getSize();

    BigDecimal getPrice();

    boolean isAvailable();

    byte[] getPhoto();

    String getPhotoContentType();
}
