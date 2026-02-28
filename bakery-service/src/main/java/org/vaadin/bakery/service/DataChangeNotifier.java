package org.vaadin.bakery.service;

/**
 * Notifies listeners of data changes for cross-session synchronization.
 * Service implementations call this after successful mutations.
 */
public interface DataChangeNotifier {

    /**
     * Broadcasts a change notification for the given entity and type to all listeners.
     *
     * @param entityType the type of entity that changed
     * @param entityId   the ID of the changed entity
     */
    void notifyChange(EntityType entityType, long entityId);

    /**
     * Broadcasts a tile-level change notification for the bakery board.
     * Default no-op for implementations that don't support tile notifications.
     *
     * @param groupingKey the tile's grouping key (after any status change)
     */
    default void notifyTileChange(String groupingKey) {
    }

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
