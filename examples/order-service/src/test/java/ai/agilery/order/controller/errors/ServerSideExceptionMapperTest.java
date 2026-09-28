package ai.agilery.order.controller.errors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ai.agilery.order.dto.ErrorResponseDto;
import ai.agilery.order.exceptions.ProcessingException;

/**
 * Unit tests for {@link ServerSideExceptionMapper}.
 *
 * <p>
 * This test class verifies that server-side exceptions are correctly mapped to
 * HTTP 500 (Internal Server Error) responses with standardized error bodies.
 * </p>
 */
class ServerSideExceptionMapperTest {

    private final ServerSideExceptionMapper exceptionMapper = new ServerSideExceptionMapper();

    /**
     * Verifies that {@link ProcessingException} is mapped to HTTP 500 (Internal
     * Server Error).
     * <p>
     * The response should contain:
     * <ul>
     * <li>The original exception message</li>
     * <li>A user-friendly generic description</li>
     * </ul>
     * </p>
     */
    @Test
    void testForProcessingException() {

        ProcessingException ex = new ProcessingException("Database connection failed");
        ResponseEntity<ErrorResponseDto> response = exceptionMapper.handleInternalServerErrorException(ex);
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertEquals("Database connection failed", response.getBody().getMessage());
        Assertions.assertEquals(
                "An unexpected error has occurred on server. Please try again in 5 to 10 minutes. "
                        + "If still problem persists contact our support team at support@agilery.ai",
                response.getBody().getDescription());
    }

    /**
     * Verifies that an unhandled {@link Exception} is mapped to HTTP 500 (Internal
     * Server Error).
     *
     * <p>
     * This ensures that unexpected runtime exceptions do not leak internal details
     * to the client and return a generic error message instead.
     * </p>
     */
    @Test
    void testForUnexpectedException() {
        Exception ex = new RuntimeException("Null pointer occurred");
        ResponseEntity<ErrorResponseDto> response = exceptionMapper.handleUnexpectedException(ex);
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertEquals("Unexpected error", response.getBody().getMessage());
        Assertions.assertEquals(
                "An unexpected error has occurred on server. Please try after 5 to 10 minutes. "
                        + "If the problem persists contact our support team at support@agilery.ai",
                response.getBody().getDescription());
    }
}
