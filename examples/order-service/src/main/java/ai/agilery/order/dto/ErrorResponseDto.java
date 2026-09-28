package ai.agilery.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object representing a standardized error response returned by
 * the API. This DTO contains a human-readable message, an optional description
 * (for additional context), and a list of validation errors when the error is
 * caused by invalid input.
 *
 * Typical usage: returned from exception handlers or controllers when an
 * operation fails and a structured error payload is required.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDto {

    /**
     * A short, user-friendly error message describing what went wrong. This is
     * intended to be displayed to callers or logged.
     */
    private String message;

    /**
     * An optional, more detailed description of the error. This can include
     * contextual information useful for debugging or explaining the cause of the
     * error to clients.
     */
    private String description;

    /**
     * When the error relates to input validation, this list contains one entry per
     * invalid field describing the field name and validation problem. If no
     * validation errors occurred, this may be null or empty.
     */
    private List<ValidationErrorDto> validationErrors;
}
