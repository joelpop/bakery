package org.vaadin.bakery.jpaservice;

import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;
import org.vaadin.bakery.jpamodel.code.OrderStatusCode;
import org.vaadin.bakery.jpamodel.entity.OrderItemEntity;

import java.util.List;

/**
 * Package-private helper that derives an order's status from its item statuses.
 * Not a Spring bean — used as a static utility by {@link JpaOrderService}.
 */
final class OrderStatusRollUpHelper {

    private OrderStatusRollUpHelper() {
        // utility class
    }

    /**
     * Derives the order-level status from the statuses of its items.
     * <p>
     * Priority cascade:
     * <ol>
     *   <li>All items CANCELED → CANCELED</li>
     *   <li>Any item PENDING_REVIEW or REJECTED → IN_REVIEW</li>
     *   <li>All non-canceled items ACCEPTED → VERIFIED</li>
     *   <li>Any item IN_PROGRESS → IN_PROGRESS</li>
     *   <li>All non-canceled items PRODUCED → PRODUCED</li>
     *   <li>Fallback: IN_REVIEW</li>
     * </ol>
     *
     * @param items the order's items
     * @return the derived order status
     */
    static OrderStatusCode deriveOrderStatus(List<OrderItemEntity> items) {
        if (items.isEmpty()) {
            return OrderStatusCode.IN_REVIEW;
        }

        var nonCanceled = items.stream()
                .filter(i -> i.getStatus() != OrderItemStatusCode.CANCELED)
                .toList();

        // All items canceled → order is canceled
        if (nonCanceled.isEmpty()) {
            return OrderStatusCode.CANCELED;
        }

        // Any item PENDING_REVIEW or REJECTED → order is IN_REVIEW
        var hasPendingOrRejected = nonCanceled.stream()
                .anyMatch(i -> i.getStatus() == OrderItemStatusCode.PENDING_REVIEW
                        || i.getStatus() == OrderItemStatusCode.REJECTED);
        if (hasPendingOrRejected) {
            return OrderStatusCode.IN_REVIEW;
        }

        // All non-canceled items are ACCEPTED → order is VERIFIED
        var allAccepted = nonCanceled.stream()
                .allMatch(i -> i.getStatus() == OrderItemStatusCode.ACCEPTED);
        if (allAccepted) {
            return OrderStatusCode.VERIFIED;
        }

        // Any item IN_PROGRESS → order is IN_PROGRESS
        var hasInProgress = nonCanceled.stream()
                .anyMatch(i -> i.getStatus() == OrderItemStatusCode.IN_PROGRESS);
        if (hasInProgress) {
            return OrderStatusCode.IN_PROGRESS;
        }

        // All non-canceled items are PRODUCED → order is PRODUCED
        var allProduced = nonCanceled.stream()
                .allMatch(i -> i.getStatus() == OrderItemStatusCode.PRODUCED);
        if (allProduced) {
            return OrderStatusCode.PRODUCED;
        }

        // Fallback (mixed ACCEPTED + PRODUCED, etc.)
        return OrderStatusCode.IN_REVIEW;
    }

    /**
     * Returns {@code true} if the given item is on hold — meaning it is ACCEPTED
     * but at least one sibling item is PENDING_REVIEW or REJECTED.
     *
     * @param item     the item to check
     * @param siblings all items in the same order (including the item itself)
     * @return true if the item is on hold
     */
    static boolean isOnHold(OrderItemEntity item, List<OrderItemEntity> siblings) {
        if (item.getStatus() != OrderItemStatusCode.ACCEPTED) {
            return false;
        }
        return siblings.stream()
                .filter(s -> !s.getId().equals(item.getId()))
                .anyMatch(s -> s.getStatus() == OrderItemStatusCode.PENDING_REVIEW
                        || s.getStatus() == OrderItemStatusCode.REJECTED);
    }
}
