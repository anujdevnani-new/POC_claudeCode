package ai.agilery.order.controller.errors;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import ai.agilery.order.dto.ErrorResponseDto;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.exceptions.ResourceNotFoundException;
import ai.agilery.order.exceptions.ValidationException;

/**
 * Unit tests for {@link ClientSideExceptionMapper}.
 *
 * <p>
 * This test class verifies that each client-side exception handled by
 * {@link ClientSideExceptionMapper} is correctly translated into a
 * {@link ResponseEntity} with the expected HTTP status code and error response
 * body.
 * </p>
 */
class ClientSideExceptionMapperTest {

    private final ClientSideExceptionMapper exceptionMapper = new ClientSideExceptionMapper();

    /**
     * Verifies that {@link MethodArgumentNotValidException} is mapped to HTTP 400
     * (Bad Request) with validation error details populated from the
     * {@link BeanPropertyBindingResult}.
     */
    @Test
    void testForMethodArgumentNotValidException() throws Exception {

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testDto");
        bindingResult.addError(new FieldError("testDto", "field", "must not be null"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ErrorResponseDto> response = exceptionMapper.handleValidationExceptions(ex);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("Request is not valid", response.getBody().getMessage());
        Assertions.assertEquals(
                "The request contains invalid or missing fields. Please check the validation errors for more details.",
                response.getBody().getDescription());
        Assertions.assertEquals(1, response.getBody().getValidationErrors().size());
    }

    /**
     * Verifies that a custom {@link ValidationException} is mapped to HTTP 400 (Bad
     * Request) and includes the provided validation errors and description.
     */
    @Test
    void testForValidationException() {
        ValidationErrorDto error = new ValidationErrorDto();
        error.setField("email");
        error.setMessage("invalid email");

        ValidationException ex = new ValidationException(List.of(error));
        ex.setDescription("Validation failed");

        ResponseEntity<ErrorResponseDto> response = exceptionMapper.handleValidationErrorsException(ex);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("Validation failed", response.getBody().getDescription());
        Assertions.assertEquals("Request is not valid. Please check the validation errors for more details.",
                response.getBody().getMessage());
        Assertions.assertEquals(1, response.getBody().getValidationErrors().size());
    }

    /**
     * Verifies that {@link MethodArgumentTypeMismatchException} is mapped to HTTP
     * 400 (Bad Request) with a descriptive validation error message indicating the
     * invalid parameter and expected type.
     */
    @Test
    void testForMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("abc", Integer.class, "id",
                null, new IllegalArgumentException());

        ResponseEntity<ErrorResponseDto> response = exceptionMapper.handleMethodArgumentTypeMismatch(ex);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertEquals("Request is not valid", response.getBody().getMessage());
        Assertions.assertEquals(
                "The request contains invalid or missing fields. Please check the validation errors for more details.",
                response.getBody().getDescription());
        Assertions.assertEquals("id", response.getBody().getValidationErrors().getFirst().getField());
        Assertions.assertEquals("Invalid value 'abc' for parameter 'id'. Expected type is 'Integer'",
                response.getBody().getValidationErrors().getFirst().getMessage());
        Assertions.assertEquals(1, response.getBody().getValidationErrors().size());
    }

    /**
     * Verifies that {@link ResourceNotFoundException} is mapped to HTTP 404 (Not
     * Found) with the correct message and description.
     */
    @Test
    void testForResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Order not found");

        ResponseEntity<ErrorResponseDto> response = exceptionMapper.handleResourceNotFound(ex);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Assertions.assertEquals("Resource not found", response.getBody().getMessage());
        Assertions.assertEquals("Order not found", response.getBody().getDescription());
    }

    /**
     * Verifies that {@link HttpMediaTypeNotSupportedException} is mapped to HTTP
     * 415 (Unsupported Media Type) with details about the unsupported content type.
     */
    @Test
    void testForHttpMediaTypeNotSupportedException() {

        MediaType unsupportedType = MediaType.APPLICATION_XML;

        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(unsupportedType,
                List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ErrorResponseDto> response = exceptionMapper.handleMediaTypeNotSupported(ex);

        Assertions.assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());

        Assertions.assertEquals("The server does not support the media type of the request.",
                response.getBody().getDescription());

        Assertions.assertEquals(
                "Content-Type not supported: application/xml. Supported content type is application/json",
                response.getBody().getMessage());
    }

    /**
     * Verifies that {@link HttpRequestMethodNotSupportedException} is mapped to
     * HTTP 405 (Method Not Allowed).
     * <p>
     * Since supported methods are normally populated by Spring internally,
     * reflection is used here to inject the supported methods without modifying
     * production code or using mocking.
     * </p>
     */
    @Test
    void testForHttpRequestMethodNotSupportedException() throws Exception {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");

        Field field = HttpRequestMethodNotSupportedException.class.getDeclaredField("supportedMethods");
        field.setAccessible(true);
        field.set(ex, new String[] { "GET" });

        ResponseEntity<ErrorResponseDto> response = exceptionMapper.handleMethodNotAllowed(ex);

        Assertions.assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        Assertions.assertEquals("The HTTP method is not allowed for the requested resource. Allowed methods are GET",
                response.getBody().getDescription());
        Assertions.assertEquals("'POST' method is not allowed", response.getBody().getMessage());
        Assertions.assertTrue(response.getBody().getMessage().contains("POST"));
        Assertions.assertTrue(response.getBody().getDescription().contains("GET"));
    }

}
