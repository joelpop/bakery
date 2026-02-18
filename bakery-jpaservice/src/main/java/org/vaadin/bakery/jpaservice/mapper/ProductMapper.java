package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.vaadin.bakery.jpamodel.entity.ProductEntity;
import org.vaadin.bakery.jpamodel.projection.ProductSelectProjection;
import org.vaadin.bakery.jpamodel.projection.ProductSummaryProjection;
import org.vaadin.bakery.uimodel.data.ProductSelect;
import org.vaadin.bakery.uimodel.data.ProductSummary;

import java.util.List;

/**
 * MapStruct mapper for product entity to UI model conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {

    /**
     * Converts a {@link ProductSummaryProjection} to a {@link ProductSummary} UI model.
     */
    @Mapping(target = "version", ignore = true)
    ProductSummary toSummary(ProductSummaryProjection projection);

    /**
     * Converts a list of {@link ProductSummaryProjection}s to a list of {@link ProductSummary} UI models.
     */
    List<ProductSummary> toSummaryList(List<ProductSummaryProjection> projections);

    /**
     * Converts a {@link ProductEntity} to a {@link ProductSummary} UI model.
     */
    ProductSummary toSummary(ProductEntity entity);

    /**
     * Converts a {@link ProductSelectProjection} to a {@link ProductSelect} UI model.
     */
    ProductSelect toSelect(ProductSelectProjection projection);

    /**
     * Converts a list of {@link ProductSelectProjection}s to a list of {@link ProductSelect} UI models.
     */
    List<ProductSelect> toSelectList(List<ProductSelectProjection> projections);

    /**
     * Updates an existing {@link ProductEntity} from a {@link ProductSummary} UI model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ProductEntity toEntity(ProductSummary summary, @MappingTarget ProductEntity entity);

    /**
     * Creates a new {@link ProductEntity} from a {@link ProductSummary} UI model.
     */
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ProductEntity toNewEntity(ProductSummary summary);
}
