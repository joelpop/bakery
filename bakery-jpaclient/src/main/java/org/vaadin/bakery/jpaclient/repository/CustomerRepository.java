package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.entity.CustomerEntity;
import org.vaadin.bakery.jpamodel.projection.CustomerSummaryProjection;

import java.util.List;
import java.util.Optional;

/**
 * Repository for customer entity operations.
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByPhoneNumber(String phoneNumber);

    Optional<CustomerEntity> findByPhoneNumberAndActiveTrue(String phoneNumber);

    List<CustomerEntity> findByNameContainingIgnoreCaseAndActiveTrueOrderByName(String name);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    List<CustomerSummaryProjection> findByActiveTrueOrderByName();

    List<CustomerSummaryProjection> findByNameContainingIgnoreCaseAndActiveTrueOrderByName(String name, Class<CustomerSummaryProjection> type);
}
