package org.vaadin.bakery.jpamodel.code;

/**
 * Order item lifecycle states for individual items within an order.
 *
 * <p>Item statuses drive the order-level status via roll-up rules. Transitions are
 * performed from the Bakery board (drag-and-drop) and from the Storefront
 * (resolve/cancel rejected items).
 */
public enum OrderItemStatusCode {
    /**
     * Item awaiting review — initial state, or re-entered after rejection correction.
     */
    PENDING_REVIEW,

    /**
     * Item reviewed and accepted — ready for production.
     */
    ACCEPTED,

    /**
     * Being manufactured — item is currently in production.
     */
    IN_PROGRESS,

    /**
     * Production completed — item has been produced.
     */
    PRODUCED,

    /**
     * Item flagged with a problem requiring storefront attention.
     */
    REJECTED,

    /**
     * Item canceled — will not be fulfilled.
     */
    CANCELED
}
