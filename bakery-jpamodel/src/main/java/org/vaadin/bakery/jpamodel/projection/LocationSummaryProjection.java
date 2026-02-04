package org.vaadin.bakery.jpamodel.projection;

/**
 * Projection for location dropdown.
 */
public interface LocationSummaryProjection {

    Long getId();

    String getName();

    String getAddress();

    String getTimezone();

    boolean isActive();

    Integer getSortOrder();
}
