package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.vaadin.bakery.jpamodel.entity.OrderEntity;
import org.vaadin.bakery.uimodel.data.OrderDashboard;
import org.vaadin.bakery.uimodel.data.OrderDetail;
import org.vaadin.bakery.uimodel.data.OrderList;

import java.util.List;

/**
 * MapStruct mapper for order entity to UI model conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {EnumMapper.class, OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "location.name", target = "locationName")
    OrderList toList(OrderEntity entity);

    List<OrderList> toListList(List<OrderEntity> entities);

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "customer.phoneNumber", target = "customerPhone")
    @Mapping(source = "location.id", target = "locationId")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(source = "createdBy.firstName", target = "createdByName")
    @Mapping(source = "updatedBy.firstName", target = "updatedByName")
    OrderDetail toDetail(OrderEntity entity);

    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(target = "itemsSummary", expression = "java(buildItemsSummary(entity))")
    OrderDashboard toDashboard(OrderEntity entity);

    List<OrderDashboard> toDashboardList(List<OrderEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OrderEntity toEntity(OrderDetail detail, @MappingTarget OrderEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toNewEntity(OrderDetail detail);

    default String buildItemsSummary(OrderEntity entity) {
        if (entity.getItems() == null || entity.getItems().isEmpty()) {
            return "";
        }
        return entity.getItems().stream()
                .map(item -> item.getQuantity() + "x " + item.getProduct().getName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
