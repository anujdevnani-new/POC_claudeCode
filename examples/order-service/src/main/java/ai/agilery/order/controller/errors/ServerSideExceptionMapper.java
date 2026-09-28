package ai.agilery.order.controller.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import ai.agilery.order.dto.ErrorResponseDto;
import ai.agilery.order.exceptions.ProcessingException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global controller advice responsible for handling server-side exceptions.
 * <p>
 * This component intercepts {@link ProcessingException} and any unexpected
 * {@link Exception} thrown within the application and maps them to a standard
 * {@link ErrorResponseDto} wrapped in a {@link ResponseEntity} with
 * {@code HTTP 500 Internal Server Error} status.
 * </p>
 * <p>
 * It ensures that the client receives a consistent error response format while
 * preventing the exposure of internal application details.
 * </p>
 */
@ControllerAdvice
@Slf4j
public class ServerSideExceptionMapper {

    /**
     * Handles {@link ProcessingException} thrown during request processing.
     * <p>
     * This method converts the exception into an {@link ErrorResponseDto}
     * containing the exception message and a generic user-friendly description. It
     * always returns an HTTP 500 response.
     * </p>
     *
     * @param ex the {@link ProcessingException} thrown during processing
     * @return a {@code ResponseEntity<ErrorResponseDto>} containing the error
     *         details
     */
    @ExceptionHandler(ProcessingException.class)
    ResponseEntity<ErrorResponseDto> handleInternalServerErrorException(ProcessingException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto();

        errorResponse.setMessage(ex.getMessage());
        errorResponse.setDescription(
                "An unexpected error has occurred on server. Please try again in 5 to 10 minutes. "
                        + "If still problem persists contact our support team at support@agilery.ai");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles any unexpected {@link Exception} that is not explicitly mapped by
     * other exception handlers.
     * <p>
     * This method logs the exception details for internal debugging and returns a
     * generic error message to the client, along with an HTTP 500 response.
     * </p>
     *
     * @param ex the unhandled {@link Exception}
     * @return a {@code ResponseEntity<ErrorResponseDto>} containing the error
     *         details
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponseDto> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error:", ex);

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setMessage("Unexpected error");
        errorResponse.setDescription(
                "An unexpected error has occurred on server. Please try after 5 to 10 minutes. "
                        + "If the problem persists contact our support team at support@agilery.ai");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
