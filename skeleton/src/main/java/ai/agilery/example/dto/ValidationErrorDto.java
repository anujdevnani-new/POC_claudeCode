package ai.agilery.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object representing a standardized validation error.
 *
 * Typical usage: Used by semantic and syntactic validation logic in the
 * service.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationErrorDto {

    /**
     * Name of the object having this validation error.
     */
    private String objectName;

    /**
     * Name of of the field in the object.
     */
    private String field;

    /**
     * The validation error message.
     */
    private String message;
}
