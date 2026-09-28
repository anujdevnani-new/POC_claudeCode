package ai.agilery.order.mapper;

import ai.agilery.order.dto.OrderItemDto;
import ai.agilery.order.entity.OrderItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link OrderItem} entity and {@link OrderItemDto}.
 * <p>
 * This interface defines the mapping rules for transforming order item data between the
 * persistence layer (entity) and the DTO layer (for API exposure).
 * </p>
 */
@Mapper(componentModel = "spring")
public interface OrderItemMapper extends EntityMapper<OrderItemDto, OrderItem> {

    /**
     * Converts an {@link OrderItemDto} to an {@link OrderItem} entity.
     * Audit fields (createdDateTime, lastModifiedDateTime, createdBy, modifiedBy) are ignored during this conversion.
     *
     * @param dto The OrderItemDto to convert.
     * @return The converted OrderItem entity.
     */
    @Override
    @Mapping(target = "createdDateTime", ignore = true)
    @Mapping(target = "lastModifiedDateTime", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toEntity(OrderItemDto dto);

    /**
     * Converts an {@link OrderItem} entity to an {@link OrderItemDto}.
     *
     * @param entity The OrderItem entity to convert.
     * @return The converted OrderItemDto.
     */
    @Override
    OrderItemDto toDto(OrderItem entity);

    /**
     * Updates an existing {@link OrderItem} entity with data from an {@link OrderItemDto}.
     * The 'id' and audit fields (createdDateTime, lastModifiedDateTime, createdBy, modifiedBy)
     * are ignored and not updated during this operation.
     *
     * @param entity The target OrderItem entity to update.
     * @param dto The source OrderItemDto containing the new data.
     */
    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDateTime", ignore = true)
    @Mapping(target = "lastModifiedDateTime", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "order", ignore = true)
    void update(@MappingTarget OrderItem entity, OrderItemDto dto);

    /**
     * Partially updates an existing {@link OrderItem} entity with non-null data from an {@link OrderItemDto}.
     * Only properties that are not null in the DTO will be mapped to the entity.
     * The 'id' and audit fields (createdDateTime, lastModifiedDateTime, createdBy, modifiedBy)
     * are ignored and not updated during this operation.
     *
     * @param entity The target OrderItem entity to patch.
     * @param dto The source OrderItemDto containing the partial update data.
     */
    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDateTime", ignore = true)
    @Mapping(target = "lastModifiedDateTime", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "order", ignore = true)
    void patch(@MappingTarget OrderItem entity, OrderItemDto dto);
}
