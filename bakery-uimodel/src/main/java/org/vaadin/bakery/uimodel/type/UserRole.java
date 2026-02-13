package org.vaadin.bakery.uimodel.type;

/**
 * UI representation of user roles.
 */
public enum UserRole {
    ADMIN("Admin", "Full system access", UserRole.ROLE_ADMIN),
    BAKER("Baker", "Kitchen staff access", UserRole.ROLE_BAKER),
    BARISTA("Barista", "Front-of-house access", UserRole.ROLE_BARISTA);

    /** Full system access — can manage users, products, locations, and all orders. */
    public static final String ROLE_ADMIN = "ADMIN";

    /** Kitchen staff access — can view products (read-only) and manage order production. */
    public static final String ROLE_BAKER = "BAKER";

    /** Front-of-house access — can create orders and manage customer interactions. */
    public static final String ROLE_BARISTA = "BARISTA";

    private final String displayName;
    private final String description;
    private final String securityName;

    UserRole(String displayName, String description, String securityName) {
        this.displayName = displayName;
        this.description = description;
        this.securityName = securityName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getSecurityName() {
        return securityName;
    }
}
