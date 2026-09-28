package ai.agilery.order.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Validation tests for the {@link MapKeyNotBlank} constraint.
 *
 * <p>
 * Verifies that maps annotated with {@link MapKeyNotBlank} contain only
 * non-null and non-blank keys.
 */
class MapKeyNotBlankTest {

    private Validator validator;

    /**
     * Dummy DTO used to apply the {@link MapKeyNotBlank} constraint.
     */
    static class TestDto {

        @MapKeyNotBlank
        private Map<String, String> attributes;

        TestDto(Map<String, String> attributes) {
            this.attributes = attributes;
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
     * Verifies that validation passes when all map keys are non-null and non-blank.
     */
    @Test
    void testForValidationSuccessWhenAllKeysAreNonBlank() {
        Map<String, String> validMap = new HashMap<>();
        validMap.put("key1", "value1");
        validMap.put("key2", "value2");

        TestDto dto = new TestDto(validMap);

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        Assertions.assertTrue(violations.isEmpty(), "No violations expected when all map keys are non-blank");
    }

    /**
     * Verifies that validation fails when a map contains an empty string as a key.
     */
    @Test
    void testForValidationFailureWhenKeyIsEmpty() {
        Map<String, String> invalidMap = new HashMap<>();
        invalidMap.put("", "value");

        TestDto dto = new TestDto(invalidMap);

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        ConstraintViolation<TestDto> message = violations.iterator().next();
        String actualMessage = message.getConstraintDescriptor().getMessageTemplate();
        String expectedMessage = "Map keys cannot be null or empty";
        Assertions.assertEquals(1, violations.size());
        Assertions.assertEquals(expectedMessage.toString(), actualMessage.toString());
    }

    /**
     * Verifies that validation fails when a map contains a blank (whitespace-only)
     * string as a key.
     */
    @Test
    void testForValidationFailureWhenKeyIsBlank() {
        Map<String, String> invalidMap = new HashMap<>();
        invalidMap.put("   ", "value");

        TestDto dto = new TestDto(invalidMap);

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        ConstraintViolation<TestDto> message = violations.iterator().next();
        String actualMessage = message.getConstraintDescriptor().getMessageTemplate();
        String expectedMessage = "Map keys cannot be null or empty";

        Assertions.assertEquals(1, violations.size());
        Assertions.assertEquals(expectedMessage.toString(), actualMessage.toString());
    }

    /**
     * Verifies that validation fails when a map contains a {@code null} key.
     */
    @Test
    void testForValidationFailureWhenKeyIsNull() {
        Map<String, String> invalidMap = new HashMap<>();
        invalidMap.put(null, "value");

        TestDto dto = new TestDto(invalidMap);

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        ConstraintViolation<TestDto> message = violations.iterator().next();
        String actualMessage = message.getConstraintDescriptor().getMessageTemplate();
        String expectedMessage = "Map keys cannot be null or empty";
        Assertions.assertEquals(1, violations.size());
        Assertions.assertEquals(expectedMessage.toString(), actualMessage.toString());

    }
}
