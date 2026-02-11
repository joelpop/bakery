package org.vaadin.bakery.service;

/**
 * Thrown when an update operation fails due to a concurrent modification
 * (optimistic locking conflict).
 */
public class StaleDataException extends RuntimeException {

    private final String entityType;
    private final Long entityId;

    /**
     * Creates a new stale data exception for the given entity type and ID.
     */
    public StaleDataException(String entityType, Long entityId) {
        super("Stale data detected for %s with id %d".formatted(entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    /**
     * Returns the type name of the entity that had a stale data conflict.
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Returns the ID of the entity that had a stale data conflict.
     */
    public Long getEntityId() {
        return entityId;
    }
}
