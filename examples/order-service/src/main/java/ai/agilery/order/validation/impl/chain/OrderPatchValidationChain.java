package ai.agilery.order.validation.impl.chain;

import org.springframework.stereotype.Component;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.validation.AbstractValidationChain;
import ai.agilery.order.validation.impl.OrderExistenceValidator;
import ai.agilery.order.validation.impl.OrderStatusTransitionValidator;
import lombok.extern.slf4j.Slf4j;

/**
 * Validation chain for patching an existing {@link OrderDto}. This chain
 * ensures that:
 * <ul>
 * <li>The order to be patched actually exists.</li>
 * <li>Any requested status change is a valid transition according to business
 * rules.</li>
 * </ul>
 */
@Component
@Slf4j
public class OrderPatchValidationChain extends AbstractValidationChain<OrderDto> {

    /**
     * Constructs an OrderPatchValidationChain with the necessary repository.
     *
     * @param orderRepository The repository for order data access.
     */
    public OrderPatchValidationChain(OrderRepository orderRepository) {
        log.info("Initializing OrderPatchValidationChain...");
        this.add(new OrderExistenceValidator<>(orderRepository, true)); // Order must exist
        this.add(new OrderStatusTransitionValidator(orderRepository)); // Validate status transitions
        log.info("OrderPatchValidationChain initialized");
    }
}
