package ai.agilery.order.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OrderStatus}.
 *
 * <p>
 * These tests verify the contract of the {@link OrderStatus} enum, ensuring
 * that all expected enum constants exist and behave as expected.
 * </p>
 */
class OrderStatusTest {

    /**
     * Verifies that all expected order statuses are present in the
     * {@link OrderStatus} enum.
     */
    @Test
    void testOrderStatus() {

        OrderStatus[] values = OrderStatus.values();

        Assertions.assertEquals(5, values.length);
        Assertions.assertEquals(OrderStatus.PENDING, values[0]);
        Assertions.assertEquals(OrderStatus.CONFIRMED, values[1]);
        Assertions.assertEquals(OrderStatus.SHIPPED, values[2]);
        Assertions.assertEquals(OrderStatus.DELIVERED, values[3]);
        Assertions.assertEquals(OrderStatus.CANCELLED, values[4]);
    }
}
