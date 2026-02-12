package org.vaadin.bakery.uimodel.type;

/**
 * UI representation of order activity types.
 */
public enum OrderActivityType {
    SYSTEM_EVENT("System Event"),
    STAFF_MESSAGE("Message");

    private final String displayName;

    OrderActivityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
