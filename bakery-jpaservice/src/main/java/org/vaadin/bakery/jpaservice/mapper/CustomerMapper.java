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

    CustomerSummary toSummary(CustomerSummaryProjection projection);

    List<CustomerSummary> toSummaryList(List<CustomerSummaryProjection> projections);

    CustomerSummary toSummary(CustomerEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "orders", ignore = true)
    CustomerEntity toEntity(CustomerSummary summary, @MappingTarget CustomerEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "orders", ignore = true)
    CustomerEntity toNewEntity(CustomerSummary summary);
}
