package ai.agilery.order.enumeration;

import java.lang.reflect.Field;
import java.util.Set;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EnumValidatorTest {

    /**
     * Test enum used to validate {@link EnumValidator} behavior.
     */
    enum TestEnum {
        A, B, C
    }

    /**
     * Dummy class used only to host the {@link ValueOfEnum} annotation for
     * reflection-based initialization.
     */
    static class DummyClass {
        @ValueOfEnum(enumClass = TestEnum.class)
        private String status;
    }

    private EnumValidator validator;
    private ConstraintValidatorContextImpl context;

    /**
     * Initializes the {@link EnumValidator} with a real {@link ValueOfEnum}
     * annotation instance and prepares a mocked validation context.
     *
     * @throws Exception if reflection access fails
     */
    @BeforeEach
    void setUp() throws Exception {
        validator = new EnumValidator();
        Field field = DummyClass.class.getDeclaredField("status");
        ValueOfEnum annotation = field.getAnnotation(ValueOfEnum.class);
        context = Mockito.mock(ConstraintValidatorContextImpl.class);
        validator.initialize(annotation);
    }

    /**
     * Verifies that {@code null} values are considered valid by the validator.
     */
    @Test
    void testForValidationSuccessWhenValueIsNull() {
        Assertions.assertTrue(validator.isValid(null, context));
    }

    /**
     * Verifies that a value matching one of the enum constants is considered valid.
     */
    @Test
    void testForValidationSuccessWhenValueIsValidEnumConstant() {
        Assertions.assertTrue(validator.isValid("A", context));
    }

    /**
     * Verifies that a value not present in the enum constants is considered invalid
     * and that appropriate message parameters are added to the validation context.
     */
    @Test
    void testForValidationFailureWhenValueIsInvalidEnumConstant() {
        Assertions.assertFalse(validator.isValid("INVALID", context));
        Mockito.verify(context).addMessageParameter("enumValues", Set.of("A", "B", "C"));
        Mockito.verify(context).addMessageParameter("invalidValue", "INVALID");
    }
}
