package ai.agilery.example.exceptions;

import java.util.List;

import ai.agilery.example.dto.ValidationErrorDto;
import lombok.Getter;
import lombok.Setter;

/**
 * Exception representing validation failures encountered during API request
 * processing.
 * <p>
 * This exception is typically thrown when one or more request parameters or
 * payload fields fail server-side validation. It encapsulates a list of
 * {@link ValidationErrorDto} objects describing each validation error in
 * detail.
 * </p>
 *
 * <p>
 * When thrown, this exception is usually handled by a controller advice (e.g.,
 * {@code ClientSideExceptionMapper}) to generate a standardized
 * {@code ErrorResponseDto} for the client.
 * </p>
 */
@Getter
@Setter
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * A human-readable description of the validation failure. This is typically
     * used to provide additional context for the client.
     */
    private String description;

    /**
     * List of validation error details representing specific issues found during
     * request validation. Each entry corresponds to a field-level or
     * parameter-level validation failure.
     */
    private List<ValidationErrorDto> validationErrors;

    /**
     * Constructs a new {@code ValidationException} with the given list of
     * validation errors.
     *
     * @param validationErrors the list of validation errors that caused the
     *                         exception; must not be {@code null}
     */
    public ValidationException(List<ValidationErrorDto> validationErrors) {
        super("Bad Request");
        this.validationErrors = validationErrors;
    }
}
