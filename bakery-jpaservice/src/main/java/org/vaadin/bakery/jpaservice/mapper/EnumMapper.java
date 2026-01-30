package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.vaadin.bakery.jpamodel.code.OrderStatusCode;
import org.vaadin.bakery.jpamodel.code.UserRoleCode;
import org.vaadin.bakery.uimodel.type.OrderStatus;
import org.vaadin.bakery.uimodel.type.UserRole;

/**
 * MapStruct mapper for enum conversions between JPA codes and UI types.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EnumMapper {

    UserRole toUserRole(UserRoleCode code);

    UserRoleCode toUserRoleCode(UserRole role);

    @ValueMapping(source = "READY_FOR_PICK_UP", target = "READY_FOR_PICK_UP")
    OrderStatus toOrderStatus(OrderStatusCode code);

    @ValueMapping(source = "READY_FOR_PICK_UP", target = "READY_FOR_PICK_UP")
    OrderStatusCode toOrderStatusCode(OrderStatus status);
}
