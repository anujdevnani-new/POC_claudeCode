package ai.agilery.order.validation.impl.chain;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.validation.ValidationHandler;
import ai.agilery.order.validation.impl.OrderHasItemsValidator;
import ai.agilery.order.validation.impl.OrderTotalAmountMatchValidator;

/**
 * Unit test for {@link OrderCreateValidationChain}.
 * <p>
 * Ensures the validation chain contains the expected number of validators
 * and that they are executed in the correct order.
 * </p>
 */
class OrderCreateValidationChainTest {

    /**
     * Verifies the order and count of validators in the order creation
     * validation chain. Any change in configuration should cause
     * this test to fail.
     */
    @Test
    void testChainOrderShouldBeCorrect() {
        // Test Data Setup
        OrderCreateValidationChain chain = new OrderCreateValidationChain();

        // Get Validation Handlers
        List<ValidationHandler<OrderDto>> handlers = chain.getChain();

        // Verify Count
        Assertions.assertEquals(2, handlers.size(),
                "OrderCreateValidationChain should have exactly 2 validation handlers");

        // Verify Order
        Assertions.assertEquals(OrderHasItemsValidator.class, handlers.get(0).getClass(),
                "First validator should be OrderHasItemsValidator");
        Assertions.assertEquals(OrderTotalAmountMatchValidator.class, handlers.get(1).getClass(),
                "Second validator should be OrderTotalAmountMatchValidator");
    }
}
