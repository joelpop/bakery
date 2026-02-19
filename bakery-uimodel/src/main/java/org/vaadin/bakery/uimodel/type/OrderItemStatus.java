package org.vaadin.bakery.uimodel.type;

/**
 * UI representation of order item statuses.
 *
 * <p>Item statuses drive order-level status via roll-up rules. Transitions are
 * performed from the Bakery board (drag-and-drop) and from the Storefront
 * (resolve/cancel rejected items).
 */
public enum OrderItemStatus {
    PENDING_REVIEW("Pending Review", "Awaiting review", "blue"),
    ACCEPTED("Accepted", "Reviewed and accepted", "green"),
    IN_PROGRESS("In Progress", "Being manufactured", "yellow"),
    PRODUCED("Produced", "Production completed", "orange"),
    REJECTED("Rejected", "Flagged with a problem", "error"),
    CANCELED("Canceled", "Item canceled", "contrast");

    private final String displayName;
    private final String description;
    private final String badgeTheme;

    OrderItemStatus(String displayName, String description, String badgeTheme) {
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
     * Returns {@code true} if this status is terminal (no further transitions from bakery board).
     */
    public boolean isTerminal() {
        return this == PRODUCED || this == CANCELED;
    }
}
