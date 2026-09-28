package ai.agilery.order.mapper;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.OrderStatus;
import ai.agilery.order.dto.PaymentMethod;
import ai.agilery.order.entity.Order;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.Named;

/**
 * MapStruct mapper for converting between {@link Order} entity and {@link OrderDto}.
 * <p>
 * This interface defines the mapping rules for transforming order data between the
 * persistence layer (entity) and the DTO layer (for API exposure). It also handles
 * the mapping of nested {@link OrderItem} and {@link OrderItemDto} objects.
 * </p>
 */
@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public abstract class OrderMapper implements EntityMapper<OrderDto, Order> {

    /**
     * Converts an {@link OrderDto} to an {@link Order} entity.
     * Audit fields (createdDateTime, lastModifiedDateTime, createdBy, modifiedBy) are ignored during this conversion.
     * Order status and payment method are explicitly mapped between DTO and entity enum types.
     *
     * @param dto The OrderDto to convert.
     * @return The converted Order entity.
     */
    @Override
    @Mapping(target = "createdDateTime", ignore = true)
    @Mapping(target = "lastModifiedDateTime", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "orderStatus", source = "orderStatus", qualifiedByName = "mapDtoOrderStatusToEntity")
    @Mapping(target = "paymentMethod", source = "paymentMethod", qualifiedByName = "mapDtoPaymentMethodToEntity")
    public abstract Order toEntity(OrderDto dto);

    /**
     * Converts an {@link Order} entity to an {@link OrderDto}.
     * Order status and payment method are explicitly mapped between entity and DTO enum types.
     *
     * @param entity The Order entity to convert.
     * @return The converted OrderDto.
     */
    @Override
    @Mapping(target = "orderStatus", source = "orderStatus", qualifiedByName = "mapEntityOrderStatusToDto")
    @Mapping(target = "paymentMethod", source = "paymentMethod", qualifiedByName = "mapEntityPaymentMethodToDto")
    public abstract OrderDto toDto(Order entity);

    /**
     * Updates an existing {@link Order} entity with data from an {@link OrderDto}.
     * The 'id' and audit fields (createdDateTime, lastModifiedDateTime, createdBy, modifiedBy)
     * are ignored and not updated during this operation.
     * Order status and payment method are explicitly mapped between DTO and entity enum types.
     *
     * @param entity The target Order entity to update.
     * @param dto The source OrderDto containing the new data.
     */
    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDateTime", ignore = true)
    @Mapping(target = "lastModifiedDateTime", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "orderStatus", source = "orderStatus", qualifiedByName = "mapDtoOrderStatusToEntity")
    @Mapping(target = "paymentMethod", source = "paymentMethod", qualifiedByName = "mapDtoPaymentMethodToEntity")
    public abstract void update(@MappingTarget Order entity, OrderDto dto);

    /**
     * Partially updates an existing {@link Order} entity with non-null data from an {@link OrderDto}.
     * Only properties that are not null in the DTO will be mapped to the entity.
     * The 'id' and audit fields (createdDateTime, lastModifiedDateTime, createdBy, modifiedBy)
     * are ignored and not updated during this operation.
     * Order status and payment method are explicitly mapped between DTO and entity enum types.
     *
     * @param entity The target Order entity to patch.
     * @param dto The source OrderDto containing the partial update data.
     */
    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDateTime", ignore = true)
    @Mapping(target = "lastModifiedDateTime", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "orderStatus", source = "orderStatus", qualifiedByName = "mapDtoOrderStatusToEntity")
    @Mapping(target = "paymentMethod", source = "paymentMethod", qualifiedByName = "mapDtoPaymentMethodToEntity")
    public abstract void patch(@MappingTarget Order entity, OrderDto dto);

    /**
     * Maps a DTO OrderStatus enum to an Entity OrderStatus enum.
     *
     * @param dtoStatus The DTO OrderStatus.
     * @return The corresponding Entity OrderStatus.
     */
    @Named("mapDtoOrderStatusToEntity")
    protected ai.agilery.order.entity.OrderStatus mapDtoOrderStatusToEntity(OrderStatus dtoStatus) {
        return dtoStatus != null ? ai.agilery.order.entity.OrderStatus.valueOf(dtoStatus.name()) : null;
    }

    /**
     * Maps an Entity OrderStatus enum to a DTO OrderStatus enum.
     *
     * @param entityStatus The Entity OrderStatus.
     * @return The corresponding DTO OrderStatus.
     */
    @Named("mapEntityOrderStatusToDto")
    protected OrderStatus mapEntityOrderStatusToDto(ai.agilery.order.entity.OrderStatus entityStatus) {
        return entityStatus != null ? OrderStatus.valueOf(entityStatus.name()) : null;
    }

    /**
     * Maps a DTO PaymentMethod enum to an Entity PaymentMethod enum.
     *
     * @param dtoPaymentMethod The DTO PaymentMethod.
     * @return The corresponding Entity PaymentMethod.
     */
    @Named("mapDtoPaymentMethodToEntity")
    protected ai.agilery.order.entity.PaymentMethod mapDtoPaymentMethodToEntity(PaymentMethod dtoPaymentMethod) {
        return dtoPaymentMethod != null ? ai.agilery.order.entity.PaymentMethod.valueOf(dtoPaymentMethod.name()) : null;
    }

    /**
     * Maps an Entity PaymentMethod enum to a DTO PaymentMethod enum.
     *
     * @param entityPaymentMethod The Entity PaymentMethod.
     * @return The corresponding DTO PaymentMethod.
     */
    @Named("mapEntityPaymentMethodToDto")
    protected PaymentMethod mapEntityPaymentMethodToDto(ai.agilery.order.entity.PaymentMethod entityPaymentMethod) {
        return entityPaymentMethod != null ? PaymentMethod.valueOf(entityPaymentMethod.name()) : null;
    }
}
