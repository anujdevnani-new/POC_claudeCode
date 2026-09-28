package ai.agilery.order.enumeration;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Unit tests for the {@link ValueOfEnum} constraint.
 *
 * <p>
 * Verifies that enum-based validation is correctly enforced by
 * {@link EnumValidator} during Bean Validation using a temporary enum and a
 * dummy DTO.
 */
class ValueOfEnumTest {

    private Validator validator;

    /**
     * Temporary enum used only for testing the {@link ValueOfEnum} constraint.
     */
    enum Status {
        CREATED, PAID, CANCELLED
    }

    /**
     * Dummy DTO used to verify that {@link ValueOfEnum} is applied correctly to a
     * field during validation.
     */
    static class TestDto {

        @ValueOfEnum(enumClass = Status.class)
        private String status;

        TestDto(String status) {
            this.status = status;
        }
    }

    /**
     * Initializes the {@link Validator} before each test execution.
     * <p>
     * This setup uses the default Bean Validation provider and does not require any
     * framework-specific configuration.
     */
    @BeforeEach
    void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Verifies that {@code null} values are considered valid by the
     * {@link ValueOfEnum} constraint.
     * <p>
     * Since {@link ValueOfEnum} does not declare {@code @NotNull}, {@code null}
     * values are allowed as per Bean Validation rules.
     */
    @Test
    void testForValidationSuccessWhenValueIsNull() {
        TestDto dto = new TestDto(null);

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        Assertions.assertTrue(violations.isEmpty(), "No violations expected when value is null");
    }

    /**
     * Verifies that validation passes when the field value matches one of the
     * allowed enum constants defined in {@link Status}.
     */
    @Test
    void testForValidationSuccessWhenValueIsValidEnumConstant() {
        TestDto dto = new TestDto("CREATED");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        Assertions.assertTrue(violations.isEmpty(), "No violations expected for valid enum value");
    }

    /**
     * Verifies that validation fails when the field value does not match any of the
     * allowed enum constants and that the correct validation message is returned.
     */
    @Test
    void testForValidationFailureWhenValueIsInvalidEnumConstant() {
        TestDto dto = new TestDto("INVALID");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        Assertions.assertEquals(1, violations.size());
        ConstraintViolation<TestDto> violation = violations.iterator().next();

        Assertions.assertEquals("Invalid value (INVALID). The allowed values are [CANCELLED, CREATED, PAID]",
                violation.getMessage());
    }
}
