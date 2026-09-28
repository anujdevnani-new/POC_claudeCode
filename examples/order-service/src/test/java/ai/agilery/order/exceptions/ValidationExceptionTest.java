package ai.agilery.order.exceptions;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ai.agilery.order.dto.ValidationErrorDto;

/**
 * Unit tests for {@link ValidationException}.
 *
 * <p>
 * These tests verify that the exception correctly initializes its default
 * message and validation error details.
 */
class ValidationExceptionTest {

    /**
     * Verifies that the constructor sets the default exception message and stores
     * the provided validation errors correctly.
     */
    @Test
    void testValidationException() {
        ValidationErrorDto error = new ValidationErrorDto();
        error.setField("email");
        error.setMessage("Email must not be empty");

        List<ValidationErrorDto> validationErrors = List.of(error);
        ValidationException exception = new ValidationException(validationErrors);
        Assertions.assertEquals("Bad Request", exception.getMessage());
        Assertions.assertEquals(validationErrors, exception.getValidationErrors());
    }

    /**
     * Verifies that {@link ValidationException} is a runtime exception.
     */
    @Test
    void testForRuntimeException() {
        ValidationException exception = new ValidationException(List.of());
        Assertions.assertTrue(exception instanceof RuntimeException);
    }
}
