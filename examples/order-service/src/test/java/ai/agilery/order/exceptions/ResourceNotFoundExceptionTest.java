package ai.agilery.order.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResourceNotFoundException}.
 *
 * <p>
 * These tests verify that the exception correctly initializes its default
 * message and description and behaves as a runtime exception.
 */
class ResourceNotFoundExceptionTest {

    /**
     * Verifies that the constructor sets the default exception message and stores
     * the provided description correctly.
     */
    @Test
    void testResourceNotFoundException() {
        String description = "Order with ID 123 does not exist";
        ResourceNotFoundException exception = new ResourceNotFoundException(description);
        Assertions.assertEquals("Resource not found", exception.getMessage());
        Assertions.assertEquals(description, exception.getDescription());
    }

    /**
     * Verifies that {@link ResourceNotFoundException} is a runtime exception.
     */
    @Test
    void testForRuntimeException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Any description");
        Assertions.assertTrue(exception instanceof RuntimeException);
    }
}
