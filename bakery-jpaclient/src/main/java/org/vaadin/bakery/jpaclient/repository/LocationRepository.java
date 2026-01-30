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

    Optional<LocationEntity> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<LocationEntity> findByActiveTrueOrderBySortOrderAsc();

    long countByActiveTrue();

    List<LocationSummaryProjection> findAllProjectedByOrderBySortOrderAsc();

    List<LocationSummaryProjection> findByActiveTrueOrderBySortOrderAsc(Class<LocationSummaryProjection> type);
}
