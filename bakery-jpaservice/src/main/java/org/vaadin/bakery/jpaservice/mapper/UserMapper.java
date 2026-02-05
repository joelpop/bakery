package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.bakery.jpamodel.entity.LocationEntity;
import org.vaadin.bakery.jpamodel.entity.UserEntity;
import org.vaadin.bakery.jpamodel.projection.UserSummaryProjection;
import org.vaadin.bakery.jpaclient.repository.LocationRepository;
import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.data.UserSummary;

import java.util.List;

/**
 * MapStruct mapper for user entity to UI model conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = EnumMapper.class)
public abstract class UserMapper {

    @Autowired
    protected LocationRepository locationRepository;

    public abstract UserSummary toSummary(UserSummaryProjection projection);

    public abstract List<UserSummary> toSummaryList(List<UserSummaryProjection> projections);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "primaryLocationId", source = "primaryLocation.id")
    public abstract UserDetail toDetail(UserEntity entity);

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "primaryLocation", source = "primaryLocationId", qualifiedByName = "locationIdToEntity")
    public abstract UserEntity toEntity(UserDetail detail, @MappingTarget UserEntity entity);

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "primaryLocation", source = "primaryLocationId", qualifiedByName = "locationIdToEntity")
    public abstract UserEntity toNewEntity(UserDetail detail);

    @Named("locationIdToEntity")
    protected LocationEntity locationIdToEntity(Long locationId) {
        if (locationId == null) {
            return null;
        }
        return locationRepository.findById(locationId).orElse(null);
    }
}
