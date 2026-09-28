package ai.agilery.order.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * Exception thrown when an unexpected processing error occurs on the server
 * side.
 * <p>
 * This exception represents internal server errors that occur during backend
 * processing, such as failures in service logic, data transformation, external
 * system integration, or any other unexpected runtime issue.
 * </p>
 *
 * <p>
 * Typically, this exception is handled by {@code ServerSideExceptionMapper},
 * which converts it into a standardized {@code ErrorResponseDto} returned with
 * HTTP status 500 (Internal Server Error).
 * </p>
 */
@Getter
@Setter
public class ProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new {@code ProcessingException} with a custom message indicating an
     * internal server error.
     *
     * @param message The error message for this exception.
     */
    public ProcessingException(String message) {
        super(message);
    }
}
