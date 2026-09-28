package ai.agilery.order.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PaymentMethod}.
 *
 * <p>
 * Verifies the contract of the {@link PaymentMethod} enum by ensuring that all
 * expected payment methods are present and in the correct order.
 * </p>
 */
class PaymentMethodTest {

    /**
     * Verifies that all expected payment methods are defined in the
     * {@link PaymentMethod} enum.
     */
    @Test
    void testPaymentMethod() {
        PaymentMethod[] values = PaymentMethod.values();

        Assertions.assertEquals(4, values.length);
        Assertions.assertEquals(PaymentMethod.CREDIT_CARD, values[0]);
        Assertions.assertEquals(PaymentMethod.DEBIT_CARD, values[1]);
        Assertions.assertEquals(PaymentMethod.CASH, values[2]);
        Assertions.assertEquals(PaymentMethod.UPI, values[3]);
    }
}
