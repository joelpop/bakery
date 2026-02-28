package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;
import org.vaadin.bakery.jpamodel.entity.OrderEntity;
import org.vaadin.bakery.uimodel.data.OrderDashboard;
import org.vaadin.bakery.uimodel.data.OrderDetail;
import org.vaadin.bakery.uimodel.data.OrderList;

import java.util.List;

/**
 * MapStruct mapper for order entity to UI model conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {EnumMapper.class, OrderItemMapper.class, InstantMapper.class})
public interface OrderMapper {

    /**
     * Converts an {@link OrderEntity} to an {@link OrderList} UI model.
     */
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(source = "createdBy.firstName", target = "createdByName")
    @Mapping(source = "updatedBy.firstName", target = "updatedByName")
    OrderList toList(OrderEntity entity);

    /**
     * Converts a list of {@link OrderEntity} instances to a list of {@link OrderList} UI models.
     */
    List<OrderList> toListList(List<OrderEntity> entities);

    /**
     * Converts an {@link OrderEntity} to an {@link OrderDetail} UI model.
     */
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "customer.phoneNumber", target = "customerPhone")
    @Mapping(source = "location.id", target = "locationId")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(source = "createdBy.firstName", target = "createdByName")
    @Mapping(source = "updatedBy.firstName", target = "updatedByName")
    @Mapping(target = "newCustomerCreated", ignore = true)
    OrderDetail toDetail(OrderEntity entity);

    /**
     * Converts an {@link OrderEntity} to an {@link OrderDashboard} UI model.
     */
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(target = "itemsSummary", expression = "java(buildItemsSummary(entity))")
    OrderDashboard toDashboard(OrderEntity entity);

    /**
     * Converts a list of {@link OrderEntity} instances to a list of {@link OrderDashboard} UI models.
     */
    List<OrderDashboard> toDashboardList(List<OrderEntity> entities);

    /**
     * Updates an existing {@link OrderEntity} from an {@link OrderDetail} UI model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OrderEntity toEntity(OrderDetail detail, @MappingTarget OrderEntity entity);

    /**
     * Creates a new {@link OrderEntity} from an {@link OrderDetail} UI model.
     */
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toNewEntity(OrderDetail detail);

    /**
     * After mapping an order to an OrderList, computes whether any item has REJECTED status.
     */
    @AfterMapping
    default void computeHasRejectedItems(OrderEntity entity, @MappingTarget OrderList orderList) {
        if (entity.getItems() != null) {
            var hasRejected = entity.getItems().stream()
                    .anyMatch(item -> item.getStatus() == OrderItemStatusCode.REJECTED);
            orderList.setHasRejectedItems(hasRejected);
        }
    }

    /**
     * Builds a comma-separated summary string of order items (e.g., "2x Croissant, 1x Baguette").
     */
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
