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

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<UserEntity> findByRole(UserRoleCode role);

    List<UserEntity> findByRoleOrderByLastNameAscFirstNameAsc(UserRoleCode role);

    long countByRole(UserRoleCode role);

    List<UserSummaryProjection> findAllProjectedBy();

    List<UserSummaryProjection> findByRoleOrderByLastNameAscFirstNameAsc(UserRoleCode role, Class<UserSummaryProjection> type);
}
