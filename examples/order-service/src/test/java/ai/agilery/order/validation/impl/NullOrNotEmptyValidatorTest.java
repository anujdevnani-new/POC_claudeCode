package ai.agilery.order.validation.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.agilery.order.validation.NullOrNotEmpty;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Unit tests for {@link NullOrNotEmptyValidator}.
 *
 * <p>
 * Verifies that a string value is considered valid when it is {@code null} or
 * contains at least one non-whitespace character, and invalid otherwise.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class NullOrNotEmptyValidatorTest {

    private final NullOrNotEmptyValidator validator = new NullOrNotEmptyValidator();

    @Mock
    private ConstraintValidatorContext context;

    /**
     * Verifies that validation succeeds when the input value is {@code null}.
     * <p>
     * A null value is allowed by the {@link NullOrNotEmpty} constraint.
     * </p>
     */
    @Test
    void testForReturnTrueWhenValueIsNull() {
        boolean actual = validator.isValid(null, context);
        boolean expected = true;
        Assertions.assertEquals(expected, actual, "Expected valid when value is null");
    }

    /**
     * Verifies that validation succeeds when the input value is a non-empty,
     * non-blank string.
     */
    @Test
    void testForReturnTrueWhenValueIsNonEmptyString() {
        String input = "Valid String";
        boolean actual = validator.isValid(input, context);
        boolean expected = true;
        Assertions.assertEquals(expected, actual, "Expected valid when value is non-empty string");
    }

    /**
     * Verifies that validation fails when the input value is an empty string.
     */
    @Test
    void testForReturnFalseWhenValueIsEmptyString() {
        String input = "";
        boolean actual = validator.isValid(input, context);
        boolean expected = false;
        Assertions.assertEquals(expected, actual, "Expected invalid when value is empty string");
    }

    /**
     * Verifies that validation fails when the input value contains only whitespace
     * characters.
     */
    @Test
    void testForReturnFalseWhenValueIsBlankString() {
        String input = "   ";
        boolean actual = validator.isValid(input, context);
        boolean expected = false;
        Assertions.assertEquals(expected, actual, "Expected invalid when value is blank string");
    }

    /**
     * Verifies that validation succeeds when the input value contains
     * non-whitespace characters with surrounding spaces.
     */
    @Test
    void testForReturnTrueWhenValueIsStringWithSpacesAndText() {
        String input = "  text ";
        boolean actual = validator.isValid(input, context);
        boolean expected = true;
        Assertions.assertEquals(expected, actual, "Expected valid when value has text with surrounding spaces");
    }
}
