package ai.agilery.order.validation;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Validation tests for the {@link NullOrNotEmpty} constraint.
 *
 * <p>
 * Verifies that fields annotated with {@link NullOrNotEmpty} accept null values
 * and non-empty strings, while rejecting empty or blank values.
 */
class NullOrNotEmptyTest {

    private Validator validator;

    /**
     * Dummy DTO used to apply the {@link NullOrNotEmpty} constraint.
     */
    static class TestDto {

        @NullOrNotEmpty
        private String value;

        TestDto(String value) {
            this.value = value;
        }
    }

    /**
     * Initializes the Bean Validation {@link Validator} before each test.
     */
    @BeforeEach
    void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Verifies that validation passes when the annotated value is {@code null}.
     */
    @Test
    void testForValidationSuccessWhenValueIsNull() {
        TestDto dto = new TestDto(null);

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        Assertions.assertTrue(violations.isEmpty(), "No violations expected when value is null");
    }

    /**
     * Verifies that validation passes when the annotated value is a non-empty
     * string.
     */
    @Test
    void testForValidationSuccessWhenValueIsNonEmpty() {
        TestDto dto = new TestDto("valid");
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        Assertions.assertTrue(violations.isEmpty(), "No violations expected when value is non-empty");
    }

    /**
     * Verifies that validation fails when the annotated value is an empty string.
     */
    @Test
    void testForValidationFailureWhenValueIsEmpty() {
        TestDto dto = new TestDto("");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        Assertions.assertEquals(1, violations.size());

        ConstraintViolation<TestDto> violation = violations.iterator().next();
        String actualMessage = violation.getConstraintDescriptor().getMessageTemplate();
        String expectedMessage = "must be either null or non-empty for this operation";
        Assertions.assertEquals(expectedMessage.toString(), actualMessage.toString());
    }

    /**
     * Verifies that validation fails when the annotated value is a blank
     * (whitespace-only) string.
     */
    @Test
    void testForValidationFailureWhenValueIsBlank() {
        TestDto dto = new TestDto("   ");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        Assertions.assertEquals(1, violations.size());

        ConstraintViolation<TestDto> violation = violations.iterator().next();
        String actualMessage = violation.getConstraintDescriptor().getMessageTemplate();
        String expectedMessage = "must be either null or non-empty for this operation";
        Assertions.assertEquals(expectedMessage.toString(), actualMessage.toString());
    }
}
