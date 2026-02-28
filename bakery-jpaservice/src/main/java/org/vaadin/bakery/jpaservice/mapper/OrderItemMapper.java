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
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {EnumMapper.class})
public interface OrderItemMapper {

    /**
     * Converts an {@link OrderItemSummaryProjection} to an {@link OrderItemSummary} UI model.
     */
    OrderItemSummary toSummary(OrderItemSummaryProjection projection);

    /**
     * Converts a list of {@link OrderItemSummaryProjection}s to a list of {@link OrderItemSummary} UI models.
     */
    List<OrderItemSummary> toSummaryList(List<OrderItemSummaryProjection> projections);

    /**
     * Converts an {@link OrderItemEntity} to an {@link OrderItemSummary} UI model.
     */
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.size", target = "productSize")
    OrderItemSummary toSummary(OrderItemEntity entity);

    /**
     * Converts an {@link OrderItemEntity} to an {@link OrderItemDetail} UI model.
     */
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.size", target = "productSize")
    OrderItemDetail toDetail(OrderItemEntity entity);

    /**
     * Converts a list of {@link OrderItemEntity} instances to a list of {@link OrderItemDetail} UI models.
     */
    List<OrderItemDetail> toDetailList(List<OrderItemEntity> entities);

    /**
     * Updates an existing {@link OrderItemEntity} from an {@link OrderItemDetail} UI model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "status", ignore = true)
    OrderItemEntity toEntity(OrderItemDetail detail, @MappingTarget OrderItemEntity entity);

    /**
     * Creates a new {@link OrderItemEntity} from an {@link OrderItemDetail} UI model.
     */
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "status", ignore = true)
    OrderItemEntity toNewEntity(OrderItemDetail detail);
}
