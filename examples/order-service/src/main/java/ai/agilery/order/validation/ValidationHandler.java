package ai.agilery.order.validation;

import java.util.List;

import ai.agilery.order.dto.ValidationErrorDto;


/**
 * Handler to validate the provided DTO.
 *
 * @param <T> The DTO class.
 */
public interface ValidationHandler<T> {
    /**
     * This method validates required aspects of the DTO and return validation
     * errors if any.
     *
     * @param dto The DTO to be validated.
     * @return The list of validation errors if any. Otherwise, an empty list is
     *         returned.
     */
    List<ValidationErrorDto> validate(T dto);
}
