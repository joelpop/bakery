package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.vaadin.bakery.jpamodel.entity.UserEntity;
import org.vaadin.bakery.jpamodel.projection.UserSummaryProjection;
import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.data.UserSummary;

import java.util.List;

/**
 * MapStruct mapper for user entity to UI model conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = EnumMapper.class)
public interface UserMapper {

    UserSummary toSummary(UserSummaryProjection projection);

    List<UserSummary> toSummaryList(List<UserSummaryProjection> projections);

    @Mapping(target = "password", ignore = true)
    UserDetail toDetail(UserEntity entity);

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    UserEntity toEntity(UserDetail detail, @MappingTarget UserEntity entity);

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "version", ignore = true)
    UserEntity toNewEntity(UserDetail detail);
}
