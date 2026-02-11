package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.entity.LocationEntity;
import org.vaadin.bakery.jpamodel.projection.LocationSummaryProjection;

import java.util.List;
import java.util.Optional;

/**
 * Repository for location entity operations.
 */
@Repository
public interface LocationRepository extends JpaRepository<LocationEntity, Long> {

    /** Checks whether a location with the given name exists. */
    boolean existsByName(String name);

    /** Checks whether a location with the given name exists, excluding the specified location ID. */
    boolean existsByNameAndIdNot(String name, Long id);

    /** Finds all active locations ordered by sort order. */
    List<LocationEntity> findByActiveTrueOrderBySortOrderAsc();

    /** Returns the count of active locations. */
    long countByActiveTrue();

    /** Returns summary projections for all locations ordered by sort order. */
    List<LocationSummaryProjection> findAllProjectedByOrderBySortOrderAsc();

    /** Returns summary projections for all active locations ordered by sort order. */
    List<LocationSummaryProjection> findByActiveTrueOrderBySortOrderAsc(Class<LocationSummaryProjection> type);
}
