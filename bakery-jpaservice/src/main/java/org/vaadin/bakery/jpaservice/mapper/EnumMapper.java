package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode;
import org.vaadin.bakery.jpamodel.code.OrderStatusCode;
import org.vaadin.bakery.jpamodel.code.UserRoleCode;
import org.vaadin.bakery.uimodel.type.OrderActivityType;
import org.vaadin.bakery.uimodel.type.OrderStatus;
import org.vaadin.bakery.uimodel.type.UserRole;

/**
 * MapStruct mapper for enum conversions between JPA codes and UI types.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EnumMapper {

    /**
     * Converts a {@link UserRoleCode} JPA code to a {@link UserRole} UI type.
     */
    UserRole toUserRole(UserRoleCode code);

    /**
     * Converts a {@link UserRole} UI type to a {@link UserRoleCode} JPA code.
     */
    UserRoleCode toUserRoleCode(UserRole role);

    /**
     * Converts an {@link OrderStatusCode} JPA code to an {@link OrderStatus} UI type.
     */
    @ValueMapping(source = "READY_FOR_PICK_UP", target = "READY_FOR_PICK_UP")
    OrderStatus toOrderStatus(OrderStatusCode code);

    /**
     * Converts an {@link OrderStatus} UI type to an {@link OrderStatusCode} JPA code.
     */
    @ValueMapping(source = "READY_FOR_PICK_UP", target = "READY_FOR_PICK_UP")
    OrderStatusCode toOrderStatusCode(OrderStatus status);

    /**
     * Converts an {@link OrderActivityTypeCode} JPA code to an {@link OrderActivityType} UI type.
     */
    OrderActivityType toOrderActivityType(OrderActivityTypeCode code);

    /**
     * Converts an {@link OrderActivityType} UI type to an {@link OrderActivityTypeCode} JPA code.
     */
    OrderActivityTypeCode toOrderActivityTypeCode(OrderActivityType type);
}
