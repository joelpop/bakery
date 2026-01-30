package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.ProductSelect;
import org.vaadin.bakery.uimodel.data.ProductSummary;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for product management operations.
 */
public interface ProductService {

    List<ProductSummary> list();

    List<ProductSelect> listAvailable();

    Optional<ProductSummary> get(Long id);

    ProductSummary create(ProductSummary product);

    ProductSummary update(Long id, ProductSummary product);

    void delete(Long id);

    long countUnavailable();

    boolean nameExists(String name);

    boolean nameExistsForOtherProduct(String name, Long productId);
}
