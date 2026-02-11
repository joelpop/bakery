package org.vaadin.bakery.service;

/**
 * Thrown when an update operation fails due to a concurrent modification
 * (optimistic locking conflict).
 */
public class StaleDataException extends RuntimeException {

    private final String entityType;
    private final Long entityId;

    public StaleDataException(String entityType, Long entityId) {
        super("Stale data detected for %s with id %d".formatted(entityType, entityId));
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }
}
