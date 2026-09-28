package ai.agilery.order.validation.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

/**
 * Unit tests for {@link MapKeyNotBlankValidator}.
 *
 * <p>
 * Verifies that map keys are validated correctly for null, empty, blank, and
 * non-string key scenarios.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class MapKeyNotBlankValidatorTest {

    private final MapKeyNotBlankValidator validator = new MapKeyNotBlankValidator();

    @Mock
    private ConstraintValidatorContext context;

    /**
     * Verifies that validation succeeds when the map is {@code null}.
     * <p>
     * A null map is considered valid by design.
     * </p>
     */
    @Test
    void testForReturnTrueWhenMapIsNull() {
        boolean actual = validator.isValid(null, context);
        boolean expected = true;
        Assertions.assertEquals(expected, actual, "Expected valid when map is null");
    }

    /**
     * Verifies that validation succeeds when the map is empty.
     * <p>
     * An empty map contains no invalid keys and should be valid.
     * </p>
     */
    @Test
    void testForReturnTrueWhenMapIsEmpty() {
        Map<String, String> map = new HashMap<>();
        boolean actual = validator.isValid(map, context);
        boolean expected = true;
        Assertions.assertEquals(expected, actual, "Expected valid when map is empty");
    }

    /**
     * Verifies that validation succeeds when all map keys are non-null and
     * non-blank strings.
     */
    @Test
    void testForReturnTrueWhenAllKeysAreNonBlank() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        boolean actual = validator.isValid(map, context);
        boolean expected = true;
        Assertions.assertEquals(expected, actual, "Expected valid when all keys are non-blank");
    }

    /**
     * Verifies that validation fails when a map contains a {@code null} key.
     */
    @Test
    void testForReturnFalseWhenKeyIsNull() {
        Map<String, String> map = new HashMap<>();
        map.put(null, "value");
        boolean actual = validator.isValid(map, context);
        boolean expected = false;
        Assertions.assertEquals(expected, actual, "Expected invalid when a key is null");
    }

    /**
     * Verifies that validation fails when a map contains an empty string key.
     */
    @Test
    void testForReturnFalseWhenKeyIsEmptyString() {
        Map<String, String> map = new HashMap<>();
        map.put("", "value");
        boolean actual = validator.isValid(map, context);
        boolean expected = false;
        Assertions.assertEquals(expected, actual, "Expected invalid when a key is empty string");
    }

    /**
     * Verifies that validation fails when a map contains a blank (whitespace-only)
     * string key.
     */
    @Test
    void testForReturnFalseWhenKeyIsBlankString() {
        Map<String, String> map = new HashMap<>();
        map.put("   ", "value");
        boolean actual = validator.isValid(map, context);
        boolean expected = false;
        Assertions.assertEquals(expected, actual, "Expected invalid when a key is blank string");
    }

    /**
     * Verifies that validation succeeds when all keys are non-string and non-null
     * values.
     */
    @Test
    void testForReturnTrueWhenKeysAreNonStringAndNonNull() {
        Map<Integer, String> map = new HashMap<>();
        map.put(123, "value");
        map.put(456, "another");
        boolean actual = validator.isValid(map, context);
        boolean expected = true;
        Assertions.assertEquals(expected, actual, "Expected valid when keys are non-string and non-null");
    }
}
