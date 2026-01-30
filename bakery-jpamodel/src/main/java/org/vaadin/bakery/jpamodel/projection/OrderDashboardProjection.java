package org.vaadin.bakery.jpamodel.projection;

import org.vaadin.bakery.jpamodel.code.OrderStatusCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Projection for dashboard upcoming orders widget.
 */
public interface OrderDashboardProjection {

    Long getId();

    OrderStatusCode getStatus();

    LocalDate getDueDate();

    LocalTime getDueTime();

    boolean isPaid();

    String getCustomerName();

    String getLocationName();

    String getItemsSummary();
}
