package ai.agilery.example.controller.errors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import ai.agilery.example.dto.ErrorResponseDto;
import ai.agilery.example.dto.ValidationErrorDto;
import ai.agilery.example.exceptions.ResourceNotFoundException;
import ai.agilery.example.exceptions.ValidationException;

/**
 * Global controller advice responsible for mapping client-side exceptions to
 * appropriate HTTP status codes and standardized {@link ErrorResponseDto}
 * responses.
 * <p>
 * This class handles validation failures, type mismatches, unsupported media
 * types, resource-not-found errors, and other client-side request issues. It
 * ensures that consumers of the API receive consistent, user-friendly error
 * messages and detailed validation error information when applicable.
 * </p>
 */
@ControllerAdvice
public class ClientSideExceptionMapper {

    /**
     * Handles validation errors triggered by Spring's method argument validation
     * mechanism.
     * <p>
     * This method is invoked when a request body or parameter annotated with
     * validation constraints fails validation. It extracts detailed field-level
     * error messages and returns them as part of a {@link ErrorResponseDto} with
     * {@code HTTP 400 Bad Request}.
     * </p>
     *
     * @param ex the validation exception containing field errors
     * @return a {@link ResponseEntity} wrapping an {@link ErrorResponseDto} with
     *         error details
     * @throws Exception rethrows any unexpected errors during processing
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponseDto> handleValidationExceptions(final MethodArgumentNotValidException ex)
            throws Exception {

        ErrorResponseDto errorResponse = new ErrorResponseDto();

        errorResponse.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");
        errorResponse.setMessage("Request is not valid");

        List<FieldError> fieldErrorList = ex.getFieldErrors();
        List<ValidationErrorDto> validationErrors = fieldErrorList.stream().map((fieldError) -> {
            ValidationErrorDto validationError = new ValidationErrorDto();
            validationError.setObjectName(ex.getObjectName());
            validationError.setField(fieldError.getField());
            validationError.setMessage(fieldError.getDefaultMessage());
            return validationError;
        }).collect(Collectors.toList());

        errorResponse.setValidationErrors(validationErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles custom {@link ValidationException} thrown by application-level
     * validators.
     * <p>
     * The error response includes a description, a general validation message, and
     * a list of validation error details, returned with
     * {@code HTTP 400 Bad Request}.
     * </p>
     *
     * @param ex the custom validation exception containing error details
     * @return a {@link ResponseEntity} containing a populated
     *         {@link ErrorResponseDto}
     */
    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ErrorResponseDto> handleValidationErrorsException(ValidationException ex) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto();
        errorResponseDto.setDescription(ex.getDescription());
        errorResponseDto.setMessage("Request Not Valid. Please check the validation errors for more details.");
        errorResponseDto.setValidationErrors(ex.getValidationErrors());

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles type mismatch errors that occur when a request parameter cannot be
     * converted to the expected type.
     * <p>
     * This method extracts the mismatched field name, provided value, and expected
     * type, returning them in a {@link ErrorResponseDto} with
     * {@code HTTP 400 Bad Request}.
     * </p>
     *
     * @param ex the exception indicating a type mismatch during parameter binding
     * @return a {@link ResponseEntity} containing the error details
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");
        errorResponse.setMessage("Request is not valid");

        List<ValidationErrorDto> validationErrors = new ArrayList<>();
        ValidationErrorDto validationError = new ValidationErrorDto();

        validationError.setField(ex.getName());
        validationError.setMessage(String.format("Invalid value '%s' for parameter '%s'. Expected type is '%s'",
                ex.getValue(), ex.getName(), getTypeName(ex)));

        validationErrors.add(validationError);
        errorResponse.setValidationErrors(validationErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles {@link ResourceNotFoundException} thrown when a requested resource
     * does not exist.
     * <p>
     * This returns a {@code HTTP 404 Not Found} response containing an
     * {@link ErrorResponseDto} with a message and description provided by the
     * exception.
     * </p>
     *
     * @param ex the exception thrown when a resource cannot be located
     * @return a {@link ResponseEntity} containing a 404 error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException ex) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto();
        errorResponseDto.setDescription(ex.getDescription());
        errorResponseDto.setMessage(ex.getMessage());

        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles {@link NoResourceFoundException} thrown when a requested resource
     * does not exist.
     * <p>
     * This returns a {@code HTTP 404 Not Found} response containing an
     * {@link ErrorResponseDto} with a message and description provided by the
     * exception.
     * </p>
     *
     * @param ex the exception thrown when a resource cannot be located
     * @return a {@link ResponseEntity} containing a 404 error response
     */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(NoResourceFoundException ex) {

        ErrorResponseDto errorResponseDto = new ErrorResponseDto();
        errorResponseDto.setDescription(ex.getResourcePath() + " not found. Please check the URL.");
        errorResponseDto.setMessage(ex.getMessage());

        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles unsupported media type errors such as sending XML or plain text when
     * the API only accepts JSON.
     * <p>
     * Responds with {@code HTTP 415 Unsupported Media Type}.
     * </p>
     *
     * @param ex the exception indicating an unsupported request Content-Type
     * @return a {@link ResponseEntity} with details of the supported media types
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponseDto> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setDescription("The server does not support the media type of the request.");
        errorResponse.setMessage(
                "Content-Type not supported: " + ex.getContentType() + ". Supported content type is application/json");

        return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * Handles HTTP method not supported errors, such as sending POST to an endpoint
     * that only supports GET.
     * <p>
     * Responds with {@code HTTP 405 Method Not Allowed} and includes the list of
     * allowed methods.
     * </p>
     *
     * @param ex the exception containing the unsupported method and supported
     *           methods
     * @return a {@link ResponseEntity} containing method not allowed error details
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponseDto> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        String allowedMethods = String.join(", ", ex.getSupportedMethods());

        errorResponse.setDescription(
                "The HTTP method is not allowed for the requested resource. Allowed methods are " + allowedMethods);
        errorResponse.setMessage(String.format("'%s' method is not allowed", ex.getMethod()));

        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Helper method to extract the expected type name from a
     * {@link MethodArgumentTypeMismatchException}.
     *
     * @param ex the mismatch exception
     * @return the simple name of the expected type, or "unknown" if unavailable
     */
    private String getTypeName(MethodArgumentTypeMismatchException ex) {
        Class<?> clazz = ex.getRequiredType();
        if (clazz != null) {
            return clazz.getSimpleName();
        } else {
            return "unknown";
        }
    }
}
