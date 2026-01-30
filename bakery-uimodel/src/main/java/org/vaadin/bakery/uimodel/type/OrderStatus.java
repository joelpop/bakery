package org.vaadin.bakery.uimodel.type;

/**
 * UI representation of order statuses.
 */
public enum OrderStatus {
    NEW("New", "Order just received", "blue"),
    VERIFIED("Verified", "Order reviewed and accepted", "green"),
    NOT_OK("Not OK", "Problem requiring attention", "red"),
    CANCELLED("Cancelled", "Order cancelled", "gray"),
    IN_PROGRESS("In Progress", "Being manufactured", "yellow"),
    BAKED("Baked", "Baking completed", "orange"),
    PACKAGED("Packaged", "Packaged for transport", "purple"),
    READY_FOR_PICK_UP("Ready", "Available for pickup", "cyan"),
    PICKED_UP("Picked Up", "Order complete", "green");

    private final String displayName;
    private final String description;
    private final String badgeColor;

    OrderStatus(String displayName, String description, String badgeColor) {
        this.displayName = displayName;
        this.description = description;
        this.badgeColor = badgeColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public boolean isTerminal() {
        return this == CANCELLED || this == PICKED_UP;
    }

    public boolean isInProduction() {
        return this == IN_PROGRESS || this == BAKED || this == PACKAGED;
    }

    public boolean isPreProduction() {
        return this == NEW || this == VERIFIED || this == NOT_OK;
    }
}
