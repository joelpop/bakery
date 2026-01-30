package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.entity.ProductEntity;
import org.vaadin.bakery.jpamodel.projection.ProductSelectProjection;
import org.vaadin.bakery.jpamodel.projection.ProductSummaryProjection;

import java.util.List;
import java.util.Optional;

/**
 * Repository for product entity operations.
 */
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<ProductEntity> findByAvailableTrueOrderByNameAsc();

    long countByAvailableFalse();

    List<ProductSummaryProjection> findAllProjectedByOrderByNameAsc();

    List<ProductSelectProjection> findByAvailableTrueOrderByNameAsc(Class<ProductSelectProjection> type);
}
