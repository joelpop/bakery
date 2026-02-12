package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.vaadin.bakery.jpamodel.entity.OrderActivityEntity;
import org.vaadin.bakery.jpamodel.entity.UserEntity;
import org.vaadin.bakery.uimodel.data.OrderActivity;

import java.util.List;

/**
 * MapStruct mapper for order activity entity to UI model conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {EnumMapper.class, InstantMapper.class})
public interface OrderActivityMapper {

    /**
     * Converts an {@link OrderActivityEntity} to an {@link OrderActivity} UI model.
     */
    @Mapping(source = "author", target = "authorName", qualifiedByName = "authorFullName")
    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "referencedItem.product.name", target = "referencedItemName")
    @Mapping(source = "referencedItem.id", target = "referencedItemId")
    OrderActivity toModel(OrderActivityEntity entity);

    /**
     * Converts a list of {@link OrderActivityEntity} instances to a list of {@link OrderActivity} UI models.
     */
    List<OrderActivity> toModelList(List<OrderActivityEntity> entities);

    /**
     * Builds the author's full name from first and last name.
     */
    @Named("authorFullName")
    default String authorFullName(UserEntity author) {
        if (author == null) {
            return null;
        }
        return author.getFirstName() + " " + author.getLastName();
    }
}
