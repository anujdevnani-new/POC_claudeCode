package ai.agilery.order.enumeration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * The constraint for enumeration.
 */
@Constraint(validatedBy = EnumValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueOfEnum {

    /**
     * The enum class to be used for validation.
     *
     * @return The enum class.
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * The validation message to be returned.
     *
     * @return The validation message.
     */
    String message() default "Invalid value ({invalidValue}). The allowed values are {enumValues}";

    /**
     * The validation groups.
     *
     * @return The validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload type that can be attached to a given constraint declaration.
     *
     * @return The payload type.
     */
    Class<? extends Payload>[] payload() default {};
}
