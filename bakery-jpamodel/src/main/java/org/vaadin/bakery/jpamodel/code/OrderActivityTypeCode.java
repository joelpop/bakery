package org.vaadin.bakery.jpamodel.code;

/**
 * Types of entries in an order's activity timeline.
 */
public enum OrderActivityTypeCode {
    /**
     * Automatically generated event recording an order change
     * (e.g., status change, field edit, item modification).
     */
    SYSTEM_EVENT,

    /**
     * Human-posted message from a staff member about the order.
     */
    STAFF_MESSAGE
}
