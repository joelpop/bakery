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

    ProductSummary toSummary(ProductSummaryProjection projection);

    List<ProductSummary> toSummaryList(List<ProductSummaryProjection> projections);

    ProductSummary toSummary(ProductEntity entity);

    ProductSelect toSelect(ProductSelectProjection projection);

    List<ProductSelect> toSelectList(List<ProductSelectProjection> projections);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    ProductEntity toEntity(ProductSummary summary, @MappingTarget ProductEntity entity);

    @Mapping(target = "version", ignore = true)
    ProductEntity toNewEntity(ProductSummary summary);
}
