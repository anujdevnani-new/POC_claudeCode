package ai.agilery.order.validation.impl.chain;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import ai.agilery.order.validation.ValidationHandler;
import ai.agilery.order.validation.impl.PaginationParameterValidator;

/**
 * Unit test for {@link OrderGetAllValidationChain}.
 * <p>
 * Ensures the validation chain contains the expected validator and that it is
 * configured in the correct order.
 * </p>
 */
class OrderGetAllValidationChainTest {

    /**
     * Verifies the order and count of validators in the order retrieval validation
     * chain. Any configuration change should cause this test to fail.
     */
    @Test
    void testChainOrderShouldBeCorrect() {
        // Test Data Setup
        OrderGetAllValidationChain chain = new OrderGetAllValidationChain();

        // Get Validation Handlers
        List<ValidationHandler<Pageable>> handlers = chain.getChain();

        // Verify Count
        Assertions.assertEquals(1, handlers.size(),
                "OrderGetAllValidationChain should have exactly 1 validation handler");

        // Verify Order
        Assertions.assertEquals(PaginationParameterValidator.class, handlers.get(0).getClass(),
                "Validator should be PaginationParameterValidator");
    }
}
