package org.vaadin.bakery.jpamodel.code;

/**
 * Order lifecycle states for the Bakery application.
 */
public enum OrderStatusCode {
    /**
     * Order just received - initial state when order is created.
     */
    NEW,

    /**
     * Order reviewed and accepted - ready for production.
     */
    VERIFIED,

    /**
     * Problem requiring attention - order has issues that need resolution.
     */
    NOT_OK,

    /**
     * Order cancelled - order will not be fulfilled.
     */
    CANCELLED,

    /**
     * Being manufactured - order is currently in production.
     */
    IN_PROGRESS,

    /**
     * Baking completed - items are baked and cooling.
     */
    BAKED,

    /**
     * Packaged for transport - items are ready for pickup.
     */
    PACKAGED,

    /**
     * Available for pickup - customer can collect their order.
     */
    READY_FOR_PICK_UP,

    /**
     * Order complete - customer has picked up their order.
     */
    PICKED_UP
}
