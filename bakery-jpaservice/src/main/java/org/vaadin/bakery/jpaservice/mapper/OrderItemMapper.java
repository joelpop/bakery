package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.vaadin.bakery.jpamodel.entity.OrderItemEntity;
import org.vaadin.bakery.jpamodel.projection.OrderItemSummaryProjection;
import org.vaadin.bakery.uimodel.data.OrderItemDetail;
import org.vaadin.bakery.uimodel.data.OrderItemSummary;

import java.util.List;

/**
 * MapStruct mapper for order item entity to UI model conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemMapper {

    OrderItemSummary toSummary(OrderItemSummaryProjection projection);

    List<OrderItemSummary> toSummaryList(List<OrderItemSummaryProjection> projections);

    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.size", target = "productSize")
    OrderItemSummary toSummary(OrderItemEntity entity);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.size", target = "productSize")
    OrderItemDetail toDetail(OrderItemEntity entity);

    List<OrderItemDetail> toDetailList(List<OrderItemEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    OrderItemEntity toEntity(OrderItemDetail detail, @MappingTarget OrderItemEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    OrderItemEntity toNewEntity(OrderItemDetail detail);
}
