package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.type.UserRole;

import java.util.Optional;

/**
 * Service interface for accessing the currently authenticated user.
 */
public interface CurrentUserService {

    Optional<String> getCurrentUserEmail();

    Optional<UserDetail> getCurrentUser();

    boolean hasRole(UserRole role);

    boolean isAdmin();

    boolean isBaker();

    boolean isBarista();
}
