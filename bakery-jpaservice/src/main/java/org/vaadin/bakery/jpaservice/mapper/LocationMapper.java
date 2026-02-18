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

    /**
     * Converts a {@link LocationSummaryProjection} to a {@link LocationSummary} UI model.
     */
    @Mapping(target = "version", ignore = true)
    LocationSummary toSummary(LocationSummaryProjection projection);

    /**
     * Converts a list of {@link LocationSummaryProjection}s to a list of {@link LocationSummary} UI models.
     */
    List<LocationSummary> toSummaryList(List<LocationSummaryProjection> projections);

    /**
     * Converts a {@link LocationEntity} to a {@link LocationSummary} UI model.
     */
    LocationSummary toSummary(LocationEntity entity);

    /**
     * Converts a list of {@link LocationEntity} instances to a list of {@link LocationSummary} UI models.
     */
    List<LocationSummary> toSummaryListFromEntities(List<LocationEntity> entities);

    /**
     * Updates an existing {@link LocationEntity} from a {@link LocationSummary} UI model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    LocationEntity toEntity(LocationSummary summary, @MappingTarget LocationEntity entity);

    /**
     * Creates a new {@link LocationEntity} from a {@link LocationSummary} UI model.
     */
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    LocationEntity toNewEntity(LocationSummary summary);
}
