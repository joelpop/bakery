package org.vaadin.bakery.jpamodel.code;

/**
 * Order item lifecycle states for individual items within an order.
 * Subset of OrderStatusCode for item-level tracking.
 */
public enum OrderItemStatusCode {
    /**
     * Item just received - initial state when order is created.
     */
    NEW,

    /**
     * Item reviewed and accepted - ready for production.
     */
    VERIFIED,

    /**
     * Problem requiring attention - item has issues that need resolution.
     */
    NOT_OK,

    /**
     * Item cancelled - will not be fulfilled.
     */
    CANCELLED,

    /**
     * Being manufactured - item is currently in production.
     */
    IN_PROGRESS,

    /**
     * Baking completed - item is baked and cooling.
     */
    BAKED
}
