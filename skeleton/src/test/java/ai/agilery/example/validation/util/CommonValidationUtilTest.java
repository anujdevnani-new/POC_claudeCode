package ai.agilery.example.validation.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.agilery.example.dto.ValidationErrorDto;
import ai.agilery.example.exceptions.ResourceNotFoundException;
import ai.agilery.example.exceptions.ValidationException;

@ExtendWith(MockitoExtension.class)
class CommonValidationUtilTest {

    /**
     * The unit to be tested.
     */
    @InjectMocks
    private CommonValidationUtil commonValidationUtil;

    @Test
    void testForThrowValidationExceptionThrowsWhenErrorsPresent() {
        List<ValidationErrorDto> errors = new ArrayList<ValidationErrorDto>();
        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setField("email");
        validationErrorDto.setMessage("Invalid email format");
        errors.add(validationErrorDto);

        ValidationException exception = Assertions.assertThrows(ValidationException.class,
                () -> commonValidationUtil.throwValidationException(errors),
                "Expected ValidationException when errors are present");

        Assertions.assertEquals(errors.toString(), exception.getValidationErrors().toString());
    }

    @Test
    void testForThrowValidationExceptionDoesNotThrowWhenErrorsEmpty() {
        List<ValidationErrorDto> errors = Collections.emptyList();

        Assertions.assertDoesNotThrow(() -> commonValidationUtil.throwValidationException(errors),
                "Expected no exception when error list is empty");
    }

    @Test
    void testForThrowResourceNotFoundExceptionThrowsWhenErrorsPresent() {
        List<ValidationErrorDto> errors = new ArrayList<ValidationErrorDto>();
        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setField("id");
        validationErrorDto.setMessage("Account not found");
        errors.add(validationErrorDto);
        String message = "Resource not found";

        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
                () -> commonValidationUtil.throwResourceNotFoundException(errors, message),
                "Expected ResourceNotFoundException when errors are present");

        Assertions.assertEquals(message.toString(), exception.getMessage().toString());
    }

    @Test
    void testForThrowResourceNotFoundExceptionDoesNotThrowWhenErrorsEmpty() {
        List<ValidationErrorDto> errors = Collections.emptyList();
        Assertions.assertDoesNotThrow(
                () -> commonValidationUtil.throwResourceNotFoundException(errors, "Resource not found"),
                "Expected no exception when error list is empty");
    }
}
