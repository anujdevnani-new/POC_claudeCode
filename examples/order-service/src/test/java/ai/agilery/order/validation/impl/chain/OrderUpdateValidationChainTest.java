package ai.agilery.order.validation.impl.chain;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.validation.ValidationHandler;
import ai.agilery.order.validation.impl.OrderExistenceValidator;
import ai.agilery.order.validation.impl.OrderTotalAmountMatchValidator;

/**
 * Unit test for {@link OrderUpdateValidationChain}.
 * <p>
 * Ensures the validation chain contains the expected validators and that they
 * are executed in the correct order.
 * </p>
 */
class OrderUpdateValidationChainTest {

    /**
     * Verifies the order and count of validators in the order update validation
     * chain. Any configuration change should cause this test to fail.
     */
    @Test
    void testChainOrderShouldBeCorrect() {
        // Test Data Setup
        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        OrderUpdateValidationChain chain = new OrderUpdateValidationChain(orderRepository);

        // Get Validation Handlers
        List<ValidationHandler<OrderDto>> handlers = chain.getChain();

        // Verify Count
        Assertions.assertEquals(2, handlers.size(),
                "OrderUpdateValidationChain should have exactly 2 validation handlers");

        // Verify Order
        Assertions.assertEquals(OrderExistenceValidator.class, handlers.get(0).getClass(),
                "First validator should be OrderExistenceValidator");

        Assertions.assertEquals(OrderTotalAmountMatchValidator.class, handlers.get(1).getClass(),
                "Second validator should be OrderTotalAmountMatchValidator");
    }
}
