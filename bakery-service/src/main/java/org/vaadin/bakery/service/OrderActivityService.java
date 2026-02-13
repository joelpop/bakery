package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.OrderActivity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Service interface for order activity timeline operations.
 */
public interface OrderActivityService {

    /**
     * Returns the full activity timeline for the given order, ordered chronologically.
     */
    List<OrderActivity> listByOrder(Long orderId);

    /**
     * Returns activities posted after the given time for incremental loading.
     */
    List<OrderActivity> listByOrderSince(Long orderId, LocalDateTime since);

    /**
     * Posts a staff message on the given order and returns the created activity.
     *
     * @param orderId the order to post on
     * @param text the message text
     * @param referencedItemId optional ID of a specific order item being referenced
     */
    OrderActivity postMessage(Long orderId, String text, Long referencedItemId);

    /**
     * Marks the given activities as read in the current user's session
     * and persists the global read flag for the first reader.
     *
     * @param activityIds the activity IDs to mark as read
     */
    void markActivitiesAsRead(Set<Long> activityIds);

    /**
     * Returns the IDs of orders (from the given set) that have unread staff messages.
     */
    Set<Long> findOrderIdsWithUnreadMessages(Collection<Long> orderIds);
}
