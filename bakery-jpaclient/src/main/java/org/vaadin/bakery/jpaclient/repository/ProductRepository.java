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

    /** Finds a product by its exact name. */
    Optional<ProductEntity> findByName(String name);

    /** Checks whether a product with the given name exists. */
    boolean existsByName(String name);

    /** Checks whether a product with the given name exists, excluding the specified product ID. */
    boolean existsByNameAndIdNot(String name, Long id);

    /** Finds all available products ordered by name ascending. */
    List<ProductEntity> findByAvailableTrueOrderByNameAsc();

    /** Returns the count of unavailable products. */
    long countByAvailableFalse();

    /** Returns summary projections for all products ordered by name ascending. */
    List<ProductSummaryProjection> findAllProjectedByOrderByNameAsc();

    /** Returns select projections for all available products ordered by name ascending. */
    List<ProductSelectProjection> findByAvailableTrueOrderByNameAsc(Class<ProductSelectProjection> type);
}
