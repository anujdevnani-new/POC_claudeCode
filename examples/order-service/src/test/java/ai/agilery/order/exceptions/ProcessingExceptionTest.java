package ai.agilery.order.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProcessingException}.
 *
 * <p>
 * These tests verify that the exception correctly initializes its message
 * through both default and parameterized constructors.
 */
class ProcessingExceptionTest {

    /**
     * Verifies that the parameterized constructor sets the provided custom error
     * message.
     */
    @Test
    void testProcessingException() {
        String customMessage = "Something went wrong while processing the request";
        ProcessingException exception = new ProcessingException(customMessage);
        Assertions.assertEquals(customMessage, exception.getMessage());
    }

}
