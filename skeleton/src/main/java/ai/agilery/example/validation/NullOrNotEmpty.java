package ai.agilery.example.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ai.agilery.example.validation.impl.NullOrNotEmptyValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NullOrNotEmptyValidator.class)
@Documented
public @interface NullOrNotEmpty {
    /**
     * The default message for constraint violation.
     * @return The message.
     */
    String message() default "must be either null or non-empty for this operation";

    /**
     * Array of validation groups for the constraint validation.
     * @return Array of validation groups.
     */
    Class<?>[] groups() default { };

    /**
     * Array of payloads used during validation.
     * @return Array of payloads.
     */
    Class<? extends Payload>[] payload() default { };
}
