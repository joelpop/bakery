package org.vaadin.bakery.uimodel.type;

/**
 * UI representation of order statuses.
 *
 * <p>Pre-production statuses are derived from item statuses via roll-up rules.
 * Post-production statuses are manual transitions.
 */
public enum OrderStatus {
    IN_REVIEW("In Review", "Order awaiting review", "blue"),
    VERIFIED("Verified", "Order reviewed and accepted", "green"),
    IN_PROGRESS("In Progress", "Being manufactured", "yellow"),
    PRODUCED("Produced", "Production completed", "orange"),
    PACKAGED("Packaged", "Packaged for transport", "purple"),
    IN_TRANSIT("In Transit", "Being transported to pickup location", "contrast"),
    READY_FOR_PICK_UP("Ready", "Available for pickup", "cyan"),
    PICKED_UP("Picked Up", "Order complete", "success"),
    CANCELED("Canceled", "Order canceled", "error");

    private final String displayName;
    private final String description;
    private final String badgeTheme;

    OrderStatus(String displayName, String description, String badgeTheme) {
        this.displayName = displayName;
        this.description = description;
        this.badgeTheme = badgeTheme;
    }

    /**
     * Returns the human-readable display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a short description of the status.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the Vaadin badge theme variant for visual styling.
     */
    public String getBadgeTheme() {
        return badgeTheme;
    }

    /**
     * Returns {@code true} if this status is terminal (no further transitions possible).
     */
    public boolean isTerminal() {
        return this == CANCELED || this == PICKED_UP;
    }

    /**
     * Returns {@code true} if this status is in the production phase.
     */
    public boolean isInProduction() {
        return this == IN_PROGRESS || this == PRODUCED || this == PACKAGED;
    }

    /**
     * Returns {@code true} if this status is in the pre-production phase.
     */
    public boolean isPreProduction() {
        return this == IN_REVIEW || this == VERIFIED;
    }

    /**
     * Returns {@code true} if this status is derived from item statuses.
     */
    public boolean isDerived() {
        return this == IN_REVIEW || this == VERIFIED || this == IN_PROGRESS
                || this == PRODUCED || this == CANCELED;
    }

    /**
     * Returns {@code true} if this status is a manual post-production transition.
     */
    public boolean isManual() {
        return this == PACKAGED || this == IN_TRANSIT
                || this == READY_FOR_PICK_UP || this == PICKED_UP;
    }
}
