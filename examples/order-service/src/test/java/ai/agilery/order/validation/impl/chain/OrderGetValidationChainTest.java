package ai.agilery.order.validation.impl.chain;

import java.util.UUID;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.validation.ValidationHandler;
import ai.agilery.order.validation.impl.OrderExistenceValidator;

/**
 * Unit test for {@link OrderGetValidationChain}.
 * <p>
 * Ensures the validation chain contains the expected validator and that it is
 * configured in the correct order.
 * </p>
 */
class OrderGetValidationChainTest {

    /**
     * Verifies the order and count of validators in the order retrieval validation
     * chain. Any configuration change should cause this test to fail.
     */
    @Test
    void testChainOrderShouldBeCorrect() {
        // Test Data Setup
        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        OrderGetValidationChain chain = new OrderGetValidationChain(orderRepository);

        // Get Validation Handlers
        List<ValidationHandler<UUID>> handlers = chain.getChain();

        // Verify Count
        Assertions.assertEquals(1, handlers.size(), "OrderGetValidationChain should have exactly 1 validation handler");

        // Verify Order
        Assertions.assertEquals(OrderExistenceValidator.class, handlers.get(0).getClass(),
                "Validator should be OrderExistenceValidator");
    }
}
