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

    /** Finds a customer by exact phone number. */
    Optional<CustomerEntity> findByPhoneNumber(String phoneNumber);

    /** Finds an active customer by exact phone number. */
    Optional<CustomerEntity> findByPhoneNumberAndActiveTrue(String phoneNumber);

    /** Finds active customers whose name contains the given string, ignoring case, ordered by name. */
    List<CustomerEntity> findByNameContainingIgnoreCaseAndActiveTrueOrderByName(String name);

    /** Checks whether a customer with the given phone number exists. */
    boolean existsByPhoneNumber(String phoneNumber);

    /** Checks whether a customer with the given phone number exists, excluding the specified customer ID. */
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    /** Returns summary projections for all active customers, ordered by name. */
    List<CustomerSummaryProjection> findByActiveTrueOrderByName();

    /** Returns summary projections for active customers whose name contains the given string, ignoring case. */
    List<CustomerSummaryProjection> findByNameContainingIgnoreCaseAndActiveTrueOrderByName(String name, Class<CustomerSummaryProjection> type);

    /**
     * Finds all active customers ordered by phone number.
     * Filtering by digits is done in the service layer.
     */
    List<CustomerEntity> findByActiveTrueOrderByPhoneNumber();
}
