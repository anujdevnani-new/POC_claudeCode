package ai.agilery.order.validation.impl.chain;

import org.springframework.stereotype.Component;

import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.validation.AbstractValidationChain;
import java.util.UUID;
import ai.agilery.order.validation.impl.OrderDeletionStatusValidator;
import ai.agilery.order.validation.impl.OrderExistenceValidator;
import lombok.extern.slf4j.Slf4j;

/**
 * Validation chain for deleting (cancelling) an {@link ai.agilery.order.entity.Order} by its ID.
 * This chain ensures that:
 * <ul>
 *     <li>The order with the given ID actually exists.</li>
 *     <li>The order's current status allows for cancellation (PENDING or CONFIRMED).</li>
 * </ul>
 */
@Component
@Slf4j
public class OrderDeleteValidationChain extends AbstractValidationChain<UUID> {


    /**
     * Constructs an OrderDeleteValidationChain with the necessary repository.
     *
     * @param orderRepository repository used to validate order existence and
     *                        deletion eligibility
     */
    public OrderDeleteValidationChain(OrderRepository orderRepository) {
        log.info("Initializing OrderDeleteValidationChain...");
        this.add(new OrderExistenceValidator<>(orderRepository, true)); // Order must exist
        this.add(new OrderDeletionStatusValidator(orderRepository)); // Validate if status allows deletion
        log.info("OrderDeleteValidationChain initialized");

    }
}
