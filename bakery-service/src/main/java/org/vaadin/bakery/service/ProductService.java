package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.ProductSelect;
import org.vaadin.bakery.uimodel.data.ProductSummary;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for product management operations.
 */
public interface ProductService {

    /**
     * Returns all products.
     */
    List<ProductSummary> list();

    /**
     * Returns only available (non-deleted) products for use in selection fields.
     */
    List<ProductSelect> listAvailable();

    /**
     * Returns the product with the given ID, if found.
     */
    Optional<ProductSummary> get(Long id);

    /**
     * Creates a new product and returns the saved result.
     */
    ProductSummary create(ProductSummary product);

    /**
     * Updates an existing product identified by the given ID.
     */
    ProductSummary update(Long id, ProductSummary product);

    /**
     * Deletes the product with the given ID.
     */
    void delete(Long id);

    /**
     * Returns the current optimistic-lock version for the given product.
     */
    Optional<Integer> getVersion(Long id);

    /**
     * Returns the number of products that are currently unavailable.
     */
    long countUnavailable();

    /**
     * Checks whether the given name is already in use by any product.
     */
    boolean nameExists(String name);

    /**
     * Checks whether the given name is in use by a product other than the specified one.
     */
    boolean nameExistsForOtherProduct(String name, Long productId);
}
