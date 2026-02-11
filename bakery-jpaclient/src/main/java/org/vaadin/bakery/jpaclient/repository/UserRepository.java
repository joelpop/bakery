package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.code.UserRoleCode;
import org.vaadin.bakery.jpamodel.entity.UserEntity;
import org.vaadin.bakery.jpamodel.projection.UserSummaryProjection;

import java.util.List;
import java.util.Optional;

/**
 * Repository for user entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /** Finds a user by exact email address. */
    Optional<UserEntity> findByEmail(String email);

    /** Finds a user by email address, ignoring case. */
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /** Checks whether a user with the given email address exists. */
    boolean existsByEmail(String email);

    /** Checks whether a user with the given email address exists, excluding the specified user ID. */
    boolean existsByEmailAndIdNot(String email, Long id);

    /** Finds all users with the given role. */
    List<UserEntity> findByRole(UserRoleCode role);

    /** Finds all users with the given role, ordered by last name then first name ascending. */
    List<UserEntity> findByRoleOrderByLastNameAscFirstNameAsc(UserRoleCode role);

    /** Returns the count of users with the given role. */
    long countByRole(UserRoleCode role);

    /** Returns summary projections for all users. */
    List<UserSummaryProjection> findAllProjectedBy();

    /** Returns summary projections for users with the given role, ordered by last name then first name ascending. */
    List<UserSummaryProjection> findByRoleOrderByLastNameAscFirstNameAsc(UserRoleCode role, Class<UserSummaryProjection> type);
}
