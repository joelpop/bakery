package org.vaadin.bakery.jpamodel.code;

/**
 * Order lifecycle states for the Bakery application.
 *
 * <p>Pre-production statuses ({@link #IN_REVIEW}, {@link #VERIFIED}, {@link #IN_PROGRESS},
 * {@link #PRODUCED}, {@link #CANCELED}) are <em>derived</em> from the aggregate of order item
 * statuses. Post-production statuses ({@link #PACKAGED}, {@link #IN_TRANSIT},
 * {@link #READY_FOR_PICK_UP}, {@link #PICKED_UP}) are manual transitions.
 */
public enum OrderStatusCode {
    /**
     * Order awaiting review — initial state, or returned to review due to rejected items.
     */
    IN_REVIEW,

    /**
     * Order reviewed and accepted — all items accepted, ready for production.
     */
    VERIFIED,

    /**
     * Being manufactured — at least one item is currently in production.
     */
    IN_PROGRESS,

    /**
     * Production completed — all non-canceled items have been produced.
     */
    PRODUCED,

    /**
     * Packaged for transport — items are ready for delivery or pickup.
     */
    PACKAGED,

    /**
     * Being transported to pickup location.
     */
    IN_TRANSIT,

    /**
     * Available for pickup — customer can collect their order.
     */
    READY_FOR_PICK_UP,

    /**
     * Order complete — customer has picked up their order.
     */
    PICKED_UP,

    /**
     * Order canceled — order will not be fulfilled.
     */
    CANCELED
}
