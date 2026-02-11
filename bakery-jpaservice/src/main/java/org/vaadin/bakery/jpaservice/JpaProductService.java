package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpaclient.repository.ProductRepository;
import org.vaadin.bakery.jpamodel.projection.ProductSelectProjection;
import org.vaadin.bakery.jpaservice.mapper.ProductMapper;
import org.vaadin.bakery.service.DataChangeNotifier;
import org.vaadin.bakery.service.ProductService;
import org.vaadin.bakery.uimodel.data.ProductSelect;
import org.vaadin.bakery.uimodel.data.ProductSummary;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of the product service.
 */
@Service
@Transactional
public class JpaProductService implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final DataChangeNotifier dataChangeNotifier;

    /** Creates the product service with injected repository, mapper, and change notifier dependencies. */
    public JpaProductService(ProductRepository productRepository, ProductMapper productMapper,
                             DataChangeNotifier dataChangeNotifier) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.dataChangeNotifier = dataChangeNotifier;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummary> list() {
        return productMapper.toSummaryList(productRepository.findAllProjectedByOrderByNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSelect> listAvailable() {
        return productMapper.toSelectList(productRepository.findByAvailableTrueOrderByNameAsc(ProductSelectProjection.class));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductSummary> get(Long id) {
        return productRepository.findById(id).map(productMapper::toSummary);
    }

    @Override
    public ProductSummary create(ProductSummary product) {
        var entity = productMapper.toNewEntity(product);
        var saved = productRepository.save(entity);
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.PRODUCT);
        return productMapper.toSummary(saved);
    }

    @Override
    public ProductSummary update(Long id, ProductSummary product) {
        var entity = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        productMapper.toEntity(product, entity);
        JpaServiceHelper.flushOrThrowStale(productRepository, "product", id);
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.PRODUCT);
        return productMapper.toSummary(entity);
    }

    @Override
    public void delete(Long id) {
        productRepository.deleteById(id);
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.PRODUCT);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> getVersion(Long id) {
        return productRepository.findById(id).map(e -> e.getVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnavailable() {
        return productRepository.countByAvailableFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nameExists(String name) {
        return productRepository.existsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nameExistsForOtherProduct(String name, Long productId) {
        return productRepository.existsByNameAndIdNot(name, productId);
    }
}
