package org.vaadin.bakery.jpamodel.projection;

/**
 * Projection for customer combo box and autocomplete.
 */
public interface CustomerSummaryProjection {

    Long getId();

    String getName();

    String getPhoneNumber();

    String getEmail();
}
