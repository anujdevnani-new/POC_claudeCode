package ai.agilery.example.validation.impl;

import java.util.Map;

import ai.agilery.example.validation.MapKeyNotBlank;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator that checks whether all keys in a {@link Map} are non-null and
 * non-blank.
 * <p>
 * This validator is intended to be used with the {@link MapKeyNotBlank}
 * constraint annotation to ensure that the keys of a map meet specific
 * validation rules:
 * </p>
 *
 * <ul>
 * <li>Keys must not be {@code null}</li>
 * <li>String keys must not be blank (i.e., empty or whitespace-only)</li>
 * </ul>
 *
 * <p>
 * If the map itself is {@code null}, it is considered valid to allow the use of
 * this validator in optional fields.
 * </p>
 */
public class MapKeyNotBlankValidator implements ConstraintValidator<MapKeyNotBlank, Map<?, ?>> {

    /**
     * Validates that all keys of the provided map are non-null and non-blank.
     *
     * @param map     the map whose keys should be validated; may be {@code null}
     * @param context the validation context used to report constraint violations
     * @return {@code true} if the map is {@code null} or if all keys are valid;
     *         {@code false} otherwise
     */
    @Override
    public boolean isValid(Map<?, ?> map, ConstraintValidatorContext context) {
        if (map == null) {
            return true;
        }
        for (Object key : map.keySet()) {
            if (key == null) {
                return false;
            }
            if (key instanceof String && ((String) key).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
