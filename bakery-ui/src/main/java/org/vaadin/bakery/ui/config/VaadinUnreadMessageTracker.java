package org.vaadin.bakery.ui.config;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Service;
import org.vaadin.bakery.service.UnreadMessageTracker;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VaadinSession-based implementation of {@link UnreadMessageTracker}.
 * <p>
 * Stores a {@link ConcurrentHashMap} of activity ID to order ID in the
 * {@link VaadinSession} attributes. This is accessible from both HTTP request
 * threads and Push threads (unlike Spring's {@code @SessionScope}).
 *
 * @see VaadinUserTimezoneService for the same pattern
 */
@Service
public class VaadinUnreadMessageTracker implements UnreadMessageTracker {

    private static final String TRACKER_ATTRIBUTE = "unreadMessageTracker";
    private static final String USER_ID_ATTRIBUTE = "unreadMessageTracker.userId";

    @Override
    public void initialize(Long userId, Map<Long, Long> activityIdToOrderId) {
        var session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_ID_ATTRIBUTE, userId);
            var tracker = new ConcurrentHashMap<>(activityIdToOrderId);
            session.setAttribute(TRACKER_ATTRIBUTE, tracker);
        }
    }

    @Override
    public boolean isInitialized() {
        var session = VaadinSession.getCurrent();
        return session != null && session.getAttribute(TRACKER_ATTRIBUTE) != null;
    }

    @Override
    public Long getUserId() {
        var session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }
        return (Long) session.getAttribute(USER_ID_ATTRIBUTE);
    }

    @Override
    public boolean isUnread(Long activityId) {
        var tracker = getTracker();
        return tracker != null && tracker.containsKey(activityId);
    }

    @Override
    public Set<Long> getUnreadOrderIds(Collection<Long> orderIds) {
        var tracker = getTracker();
        if (tracker == null || tracker.isEmpty()) {
            return Set.of();
        }
        var orderIdSet = new HashSet<>(orderIds);
        var result = new HashSet<Long>();
        for (var orderId : tracker.values()) {
            if (orderIdSet.contains(orderId)) {
                result.add(orderId);
            }
        }
        return result;
    }

    @Override
    public void markAsRead(Set<Long> activityIds) {
        var tracker = getTracker();
        if (tracker != null) {
            activityIds.forEach(tracker::remove);
        }
    }

    @Override
    public void addUnread(Long activityId, Long orderId) {
        var tracker = getTracker();
        if (tracker != null) {
            tracker.put(activityId, orderId);
        }
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Long, Long> getTracker() {
        var session = VaadinSession.getCurrent();
        if (session == null) {
            return null;
        }
        return (ConcurrentHashMap<Long, Long>) session.getAttribute(TRACKER_ATTRIBUTE);
    }
}
