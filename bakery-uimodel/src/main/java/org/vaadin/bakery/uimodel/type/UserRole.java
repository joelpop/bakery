package org.vaadin.bakery.uimodel.type;

/**
 * UI representation of user roles.
 */
public enum UserRole {
    ADMIN("Admin", "Full system access"),
    BAKER("Baker", "Kitchen staff access"),
    BARISTA("Barista", "Front-of-house access");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
