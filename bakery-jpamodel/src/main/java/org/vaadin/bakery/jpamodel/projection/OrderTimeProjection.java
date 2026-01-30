package org.vaadin.bakery.jpamodel.projection;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Projection for dashboard KPI time-based queries.
 */
public interface OrderTimeProjection {

    LocalDate getDueDate();

    LocalTime getDueTime();
}
