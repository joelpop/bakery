package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.data.UserSummary;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for user management operations.
 */
public interface UserService {

    /**
     * Returns all users.
     */
    List<UserSummary> list();

    /**
     * Searches for users matching the given query string.
     */
    List<UserSummary> search(String query);

    /**
     * Returns the full user detail for the given ID, if found.
     */
    Optional<UserDetail> get(Long id);

    /**
     * Returns the full user detail for the given email address, if found.
     */
    Optional<UserDetail> getByEmail(String email);

    /**
     * Creates a new user and returns the saved result.
     */
    UserDetail create(UserDetail user);

    /**
     * Updates an existing user identified by the given ID.
     */
    UserDetail update(Long id, UserDetail user);

    /**
     * Deletes the user with the given ID.
     */
    void delete(Long id);

    /**
     * Changes the password for the user with the given ID.
     */
    void changePassword(Long id, String newPassword);

    /**
     * Returns the current optimistic-lock version for the given user.
     */
    Optional<Integer> getVersion(Long id);

    /**
     * Checks whether the given email address is already in use by any user.
     */
    boolean emailExists(String email);

    /**
     * Checks whether the given email address is in use by a user other than the specified one.
     */
    boolean emailExistsForOtherUser(String email, Long userId);
}
