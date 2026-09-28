package ai.agilery.order.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Contract for mapping between a Data Transfer Object (DTO) and its
 * corresponding Entity.
 * <p>
 * Provides methods for converting single objects as well as collections in both
 * directions. Also includes specialized methods for partial updates (patch) and
 * full updates, commonly used in service-layer operations.
 * </p>
 *
 * @param <D> the DTO type
 * @param <E> the Entity type
 */
public interface EntityMapper<D, E> {

    /**
     * Converts a DTO instance to its corresponding Entity.
     *
     * @param dto the DTO to convert
     * @return the mapped Entity instance
     */
    E toEntity(D dto);

    /**
     * Converts an Entity instance to its corresponding DTO.
     *
     * @param entity the Entity to convert
     * @return the mapped DTO instance
     */
    D toDto(E entity);

    /**
     * Converts a list of DTOs to a list of Entities.
     *
     * @param dtos the list of DTOs to convert
     * @return a list of mapped Entity instances
     */
    List<E> toEntity(List<D> dtos);

    /**
     * Converts a list of Entities to a list of DTOs.
     *
     * @param entities the list of Entities to convert
     * @return a list of mapped DTO instances
     */
    List<D> toDto(List<E> entities);

    /**
     * Applies a partial update (patch) to an existing Entity using the values from
     * the provided DTO. Only non-null properties from the DTO are applied.
     * <p>
     * This method is typically used for HTTP PATCH operations.
     * </p>
     *
     * @param entity the target Entity to be patched
     * @param dto    the DTO containing updated fields
     */
    @Named("patch")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patch(@MappingTarget E entity, D dto);

    /**
     * Applies a full update to an existing Entity using the values from the DTO.
     * All mapped fields from the DTO will overwrite corresponding Entity fields.
     * <p>
     * This method is typically used for HTTP PUT operations.
     * </p>
     *
     * @param entity the target Entity to be updated
     * @param dto    the DTO containing updated values
     */
    @Named("update")
    void update(@MappingTarget E entity, D dto);
}
