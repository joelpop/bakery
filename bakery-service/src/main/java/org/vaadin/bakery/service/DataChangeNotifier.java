package org.vaadin.bakery.service;

/**
 * Notifies listeners of data changes for cross-session synchronization.
 * Service implementations call this after successful mutations.
 */
public interface DataChangeNotifier {

    void notifyChange(EntityType entityType);

    enum EntityType {
        ORDER,
        USER,
        PRODUCT,
        LOCATION
    }
}
