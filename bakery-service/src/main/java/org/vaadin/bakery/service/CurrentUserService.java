package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.type.UserRole;

import java.util.Optional;

/**
 * Service interface for accessing the currently authenticated user.
 */
public interface CurrentUserService {

    /**
     * Returns the email address of the currently authenticated user.
     */
    Optional<String> getCurrentUserEmail();

    /**
     * Returns the full user details for the currently authenticated user.
     */
    Optional<UserDetail> getCurrentUser();

    /**
     * Checks whether the currently authenticated user has the specified role.
     */
    boolean hasRole(UserRole role);

    /**
     * Checks whether the currently authenticated user has the admin role.
     */
    boolean isAdmin();

    /**
     * Checks whether the currently authenticated user has the baker role.
     */
    boolean isBaker();

    /**
     * Checks whether the currently authenticated user has the barista role.
     */
    boolean isBarista();
}
