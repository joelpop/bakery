package org.vaadin.bakery.uimodel.data;

import java.time.LocalDateTime;

/**
 * Base class for UI models that include audit information.
 * Uses LocalDateTime for display purposes (converted from Instant at the service layer).
 */
public abstract class AbstractAuditableModel {

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByName;
    private String updatedByName;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }
}
