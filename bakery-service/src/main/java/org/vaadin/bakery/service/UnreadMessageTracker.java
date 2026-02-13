package org.vaadin.bakery.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Session-scoped tracker for unread message activity IDs.
 * <p>
 * Each user session maintains its own collection of unread message IDs.
 * On first access, the collection is populated with globally-unread messages.
 * Messages are removed when the user actually sees them (visibility-based).
 * The first user to see a message also marks it globally read, but other
 * active sessions keep their own independent copy until they too see it.
 */
public interface UnreadMessageTracker {

    /**
     * Populates the tracker with the initial set of unread messages and stores
     * the current user's ID for use in subsequent calls (including Push threads
     * where Spring Security context is not available).
     *
     * @param userId the current user's ID
     * @param activityIdToOrderId mapping of activity ID to its order ID
     */
    void initialize(Long userId, Map<Long, Long> activityIdToOrderId);

    /**
     * Returns whether the tracker has been initialized for the current session.
     */
    boolean isInitialized();

    /**
     * Returns the user ID stored during initialization. Safe to call from
     * Push threads where Spring Security context is not available.
     *
     * @return the user ID, or {@code null} if not initialized
     */
    Long getUserId();

    /**
     * Returns whether the given activity is unread in the current session.
     *
     * @param activityId the activity ID to check
     */
    boolean isUnread(Long activityId);

    /**
     * Returns the subset of the given order IDs that have unread activities
     * in the current session.
     *
     * @param orderIds the order IDs to check
     * @return order IDs that have at least one unread activity
     */
    Set<Long> getUnreadOrderIds(Collection<Long> orderIds);

    /**
     * Removes the given activity IDs from the unread collection.
     *
     * @param activityIds the activity IDs to mark as read
     */
    void markAsRead(Set<Long> activityIds);

    /**
     * Adds a newly discovered unread message to the collection.
     *
     * @param activityId the activity ID
     * @param orderId the order ID the activity belongs to
     */
    void addUnread(Long activityId, Long orderId);
}
