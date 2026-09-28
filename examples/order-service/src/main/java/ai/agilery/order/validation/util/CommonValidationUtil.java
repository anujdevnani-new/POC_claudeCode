package ai.agilery.order.validation.util;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.exceptions.ResourceNotFoundException;
import ai.agilery.order.exceptions.ValidationException;

/**
 * Utility component for throwing standardized exceptions based on validation
 * results.
 * <p>
 * This class centralizes the logic for raising exceptions such as
 * {@link ValidationException} and {@link ResourceNotFoundException} when
 * validation rules fail. It helps maintain consistent exception-handling
 * patterns across the application.
 * </p>
 */
@Component
public class CommonValidationUtil {

    /**
     * Throws a {@link ValidationException} if the provided list of validation
     * errors is not empty.
     *
     * @param errors the list of validation errors; if non-empty, a
     *               {@code ValidationException} will be thrown
     * @throws ValidationException when one or more validation errors exist
     */
    public void throwValidationException(List<ValidationErrorDto> errors) {
        if (errors.size() > 0) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Throws a {@link ResourceNotFoundException} if the provided list of validation
     * errors is not empty.
     * <p>
     * This method is used in cases where validation determines that a resource
     * referenced in the request does not exist.
     * </p>
     *
     * @param errors  the list of validation errors; if non-empty, a
     *                {@code ResourceNotFoundException} will be thrown
     * @param message a description used for constructing the exception
     * @throws ResourceNotFoundException when one or more validation errors indicate
     *                                   that a resource could not be found
     */
    public void throwResourceNotFoundException(List<ValidationErrorDto> errors, String message) {
        if (errors.size() > 0) {
            throw new ResourceNotFoundException(String.format(message));
        }
    }
}
