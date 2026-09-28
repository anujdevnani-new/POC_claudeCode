package ai.agilery.order.validation.impl.chain;

import java.util.UUID;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.validation.ValidationHandler;
import ai.agilery.order.validation.impl.OrderDeletionStatusValidator;
import ai.agilery.order.validation.impl.OrderExistenceValidator;

/**
 * Unit test for {@link OrderDeleteValidationChain}.
 * <p>
 * Ensures the validation chain contains the expected number of validators
 * and that they are executed in the correct order.
 * </p>
 */
class OrderDeleteValidationChainTest {

    /**
     * Verifies the order and count of validators in the order deletion
     * validation chain. Any change in configuration should cause
     * this test to fail.
     */
    @Test
    void testChainOrderShouldBeCorrect() {
        // Test Data Setup
        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        OrderDeleteValidationChain chain = new OrderDeleteValidationChain(orderRepository);

        // Get Validation Handlers
        List<ValidationHandler<UUID>> handlers = chain.getChain();

        // Verify Count
        Assertions.assertEquals(2, handlers.size(),
                "OrderDeleteValidationChain should have exactly 2 validation handlers");

        // Verify Order
        Assertions.assertEquals(OrderExistenceValidator.class, handlers.get(0).getClass(),
                "First validator should be OrderExistenceValidator");
        Assertions.assertEquals(OrderDeletionStatusValidator.class, handlers.get(1).getClass(),
                "Second validator should be OrderDeletionStatusValidator");
    }
}
