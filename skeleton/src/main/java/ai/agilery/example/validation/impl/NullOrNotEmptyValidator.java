package ai.agilery.example.validation.impl;

import java.util.Objects;

import org.apache.logging.log4j.util.Strings;

import ai.agilery.example.validation.NullOrNotEmpty;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for the {@link NullOrNotEmpty} constraint.
 * <p>
 * This validator ensures that the annotated string value is either:
 * </p>
 * <ul>
 * <li>{@code null}, or</li>
 * <li>non-empty and not blank (i.e., contains at least one non-whitespace
 * character)</li>
 * </ul>
 *
 * <p>
 * This is useful when a field is optional but, if provided, must contain
 * meaningful (non-whitespace) content.
 * </p>
 */
public class NullOrNotEmptyValidator implements ConstraintValidator<NullOrNotEmpty, String> {

    /**
     * Validates the given value according to the {@link NullOrNotEmpty} constraint.
     *
     * @param value   the string value to validate; may be {@code null}
     * @param context the context in which the constraint is evaluated
     * @return {@code true} if the value is {@code null} or contains at least one
     *         non-whitespace character; {@code false} otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return Objects.isNull(value) || Strings.isNotBlank(value.trim());
    }
}
