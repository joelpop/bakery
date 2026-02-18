package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.vaadin.bakery.jpamodel.entity.CustomerEntity;
import org.vaadin.bakery.jpamodel.projection.CustomerSummaryProjection;
import org.vaadin.bakery.uimodel.data.CustomerSummary;

import java.util.List;

/**
 * MapStruct mapper for customer entity to UI model conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {

    /**
     * Converts a {@link CustomerSummaryProjection} to a {@link CustomerSummary} UI model.
     */
    CustomerSummary toSummary(CustomerSummaryProjection projection);

    /**
     * Converts a list of {@link CustomerSummaryProjection}s to a list of {@link CustomerSummary} UI models.
     */
    List<CustomerSummary> toSummaryList(List<CustomerSummaryProjection> projections);

    /**
     * Converts a {@link CustomerEntity} to a {@link CustomerSummary} UI model.
     */
    CustomerSummary toSummary(CustomerEntity entity);

    /**
     * Updates an existing {@link CustomerEntity} from a {@link CustomerSummary} UI model.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    CustomerEntity toEntity(CustomerSummary summary, @MappingTarget CustomerEntity entity);

    /**
     * Creates a new {@link CustomerEntity} from a {@link CustomerSummary} UI model.
     */
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    CustomerEntity toNewEntity(CustomerSummary summary);
}
