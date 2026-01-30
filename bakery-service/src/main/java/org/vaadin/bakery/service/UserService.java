package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.data.UserSummary;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for user management operations.
 */
public interface UserService {

    List<UserSummary> list();

    List<UserSummary> search(String query);

    Optional<UserDetail> get(Long id);

    Optional<UserDetail> getByEmail(String email);

    UserDetail create(UserDetail user);

    UserDetail update(Long id, UserDetail user);

    void delete(Long id);

    void changePassword(Long id, String newPassword);

    boolean emailExists(String email);

    boolean emailExistsForOtherUser(String email, Long userId);
}
