package org.vaadin.bakery.service;

/**
 * Notifies listeners of data changes for cross-session synchronization.
 * Service implementations call this after successful mutations.
 */
public interface DataChangeNotifier {

    /**
     * Broadcasts a change notification for the given entity type to all listeners.
     */
    void notifyChange(EntityType entityType);

    /**
     * Broadcasts a message notification for targeted toast delivery.
     * Default no-op for implementations that don't support message notifications.
     */
    default void notifyMessage(MessageNotification notification) {
    }

    enum EntityType {
        ORDER,
        USER,
        PRODUCT,
        LOCATION,
        MESSAGE
    }
}
