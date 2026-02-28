package org.vaadin.bakery.uimodel.type;

import java.util.EnumSet;
import java.util.Set;

/**
 * UI representation of order item statuses.
 *
 * <p>Item statuses drive order-level status via roll-up rules. Transitions are
 * performed from the Bakery board (drag-and-drop) and from the Storefront
 * (resolve/cancel rejected items).
 */
public enum OrderItemStatus {
    PENDING_REVIEW("Pending Review", "Awaiting review", "blue"),
    REJECTED("Rejected", "Flagged with a problem", "error"),
    ACCEPTED("Accepted", "Reviewed and accepted", "green"),
    IN_PROGRESS("In Progress", "Being manufactured", "yellow"),
    PRODUCED("Produced", "Production completed", "orange"),
    CANCELED("Canceled", "Item canceled", "contrast");

    static {
        PENDING_REVIEW.validBakeryTargets = EnumSet.of(ACCEPTED, REJECTED);
        REJECTED.validBakeryTargets = EnumSet.noneOf(OrderItemStatus.class);
        ACCEPTED.validBakeryTargets = EnumSet.of(IN_PROGRESS, REJECTED);
        IN_PROGRESS.validBakeryTargets = EnumSet.of(PRODUCED);
        PRODUCED.validBakeryTargets = EnumSet.noneOf(OrderItemStatus.class);
        CANCELED.validBakeryTargets = EnumSet.noneOf(OrderItemStatus.class);
    }

    private final String displayName;
    private final String description;
    private final String badgeTheme;
    private Set<OrderItemStatus> validBakeryTargets;

    OrderItemStatus(String displayName, String description, String badgeTheme) {
        this.displayName = displayName;
        this.description = description;
        this.badgeTheme = badgeTheme;
       validBakeryTargets = Set.of();
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

    /**
     * Returns the set of statuses this status can transition to on the bakery board.
     *
     * <p>These are the static transition rules. Dynamic constraints (today-only
     * for IN_PROGRESS, hold enforcement) are applied at the view level.
     */
    public Set<OrderItemStatus> getValidBakeryTargets() {
        return validBakeryTargets;
    }
}
