package org.vaadin.bakery.ui.event;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.vaadin.bakery.service.MessageNotification;
import org.vaadin.bakery.uimodel.type.UserRole;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Broadcasts message notifications to registered UI sessions.
 * Each UI registers on attach with its session info (user ID, role, location).
 * When a message is posted, matching sessions receive a toast notification.
 */
public final class MessageBroadcaster {

    private static final ConcurrentHashMap<UI, SessionInfo> SESSIONS = new ConcurrentHashMap<>();

    private MessageBroadcaster() {
    }

    /**
     * Registers a UI session for message notifications.
     */
    public static void register(UI ui, SessionInfo sessionInfo) {
        SESSIONS.put(ui, sessionInfo);
    }

    /**
     * Unregisters a UI session.
     */
    public static void unregister(UI ui) {
        SESSIONS.remove(ui);
    }

    /**
     * Updates the current location for a registered UI session.
     */
    public static void updateLocation(UI ui, Long locationId) {
        SESSIONS.computeIfPresent(ui, (_, info) ->
                new SessionInfo(info.userId(), info.role(), locationId));
    }

    /**
     * Broadcasts a message notification to all matching sessions.
     * Skips the author's own session. Targets admins, bakers at the bakery, and
     * baristas at the order's pickup location.
     */
    public static void broadcast(MessageNotification notification) {
        SESSIONS.forEach((ui, info) -> {
            // Skip the author
            if (info.userId().equals(notification.authorId())) {
                return;
            }

            // Check audience: admin sees all, baker at bakery, barista at pickup location
            if (!isInAudience(info, notification.orderLocationId())) {
                return;
            }

            try {
                ui.access(() -> showToast(ui, notification));
            } catch (Exception _) {
                // UI may be detached; remove stale entry
                SESSIONS.remove(ui);
            }
        });
    }

    private static boolean isInAudience(SessionInfo info, Long orderLocationId) {
        return switch (info.role()) {
            case ADMIN -> true;
            case BAKER, BARISTA -> info.currentLocationId() != null
                    && info.currentLocationId().equals(orderLocationId);
        };
    }

    private static void showToast(UI ui, MessageNotification notification) {
        var text = notification.authorName() + " on Order #" + notification.orderId()
                + ": " + notification.textPreview();
        var toast = Notification.show(text, 5000, Notification.Position.TOP_END);
        toast.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
    }

    /**
     * Session info for audience targeting.
     *
     * @param userId the user's ID
     * @param role the user's role
     * @param currentLocationId the user's currently selected location
     */
    public record SessionInfo(Long userId, UserRole role, Long currentLocationId) {
    }
}
