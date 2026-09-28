package ai.agilery.example.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * Exception thrown when the requested resource cannot be found.
 * <p>
 * This exception is typically used to indicate that a client has requested an
 * entity or resource that does not exist in the system. It carries a
 * human-readable description that provides additional context about the missing
 * resource.
 * </p>
 *
 * <p>
 * This exception is generally handled by a controller advice such as
 * {@code ClientSideExceptionMapper} to return a standardized
 * {@code ErrorResponseDto} with an HTTP 404 (Not Found) status.
 * </p>
 */
@Getter
@Setter
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Additional description providing context about why the resource was not
     * found. This may include details such as the resource identifier or type.
     */
    private String description;

    /**
     * Creates a new {@code ResourceNotFoundException} with the specified
     * description.
     *
     * @param description human-readable explanation of the missing resource; must
     *                    not be {@code null}
     */
    public ResourceNotFoundException(String description) {
        super("Resource not found");
        this.description = description;
    }
}
