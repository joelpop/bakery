package org.vaadin.bakery.jpamodel.code;

/**
 * User authorization roles for the Bakery application.
 */
public enum UserRoleCode {
    /**
     * Full system access - can manage users, products, locations, and all orders.
     */
    ADMIN,

    /**
     * Kitchen staff access - can view products (read-only) and manage order production.
     */
    BAKER,

    /**
     * Front-of-house access - can create orders and manage customer interactions.
     */
    BARISTA
}
