package ai.agilery.order.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ai.agilery.order.validation.impl.MapKeyNotBlankValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validation annotation to ensure that all keys in a {@link java.util.Map} are
 * non-null and non-blank.
 * <p>
 * This constraint is applied to fields or parameters of type {@code Map<?, ?>}
 * and ensures that:
 * </p>
 * <ul>
 * <li>No map key is {@code null}</li>
 * <li>If a key is a {@link String}, it must not be empty or
 * whitespace-only</li>
 * </ul>
 *
 * <p>
 * The actual validation logic is implemented in
 * {@link MapKeyNotBlankValidator}.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * {@code
 * @MapKeyNotBlank
 * private Map<String, String> attributes;
 * }
 * </pre>
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MapKeyNotBlankValidator.class)
public @interface MapKeyNotBlank {

    /**
     * The default validation error message.
     *
     * @return the error message to be used when validation fails
     */
    String message() default "Map keys cannot be null or empty";

    /**
     * Allows specification of validation groups for advanced validation scenarios.
     *
     * @return the validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Can be used by clients of the Bean Validation API to assign custom payload
     * objects to a constraint.
     *
     * @return the payload type
     */
    Class<? extends Payload>[] payload() default {};
}
