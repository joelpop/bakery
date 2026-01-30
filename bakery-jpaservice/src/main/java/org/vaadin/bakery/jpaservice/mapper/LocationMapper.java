package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.vaadin.bakery.jpamodel.entity.LocationEntity;
import org.vaadin.bakery.jpamodel.projection.LocationSummaryProjection;
import org.vaadin.bakery.uimodel.data.LocationSummary;

import java.util.List;

/**
 * MapStruct mapper for location entity to UI model conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LocationMapper {

    LocationSummary toSummary(LocationSummaryProjection projection);

    List<LocationSummary> toSummaryList(List<LocationSummaryProjection> projections);

    LocationSummary toSummary(LocationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    LocationEntity toEntity(LocationSummary summary, @MappingTarget LocationEntity entity);

    @Mapping(target = "version", ignore = true)
    LocationEntity toNewEntity(LocationSummary summary);
}
