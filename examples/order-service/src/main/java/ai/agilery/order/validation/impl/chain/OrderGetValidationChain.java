package ai.agilery.order.validation.impl.chain;

import org.springframework.stereotype.Component;

import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.validation.AbstractValidationChain;
import java.util.UUID;
import ai.agilery.order.validation.impl.OrderExistenceValidator;
import lombok.extern.slf4j.Slf4j;

/**
 * Validation chain for retrieving an {@link ai.agilery.order.entity.Order} by
 * its ID. This chain ensures that:
 * <ul>
 * <li>The order with the given ID actually exists.</li>
 * </ul>
 */
@Component
@Slf4j
public class OrderGetValidationChain extends AbstractValidationChain<UUID> {

    /**
     * Constructs an OrderGetValidationChain with the necessary repository.
     *
     * @param orderRepository repository used to validate
     *                        the existence of an order
     */
    public OrderGetValidationChain(OrderRepository orderRepository) {
        log.info("Initializing OrderGetValidationChain...");
        this.add(new OrderExistenceValidator<>(orderRepository, true)); // Order must exist
        log.info("OrderGetValidationChain initialized");
    }

}
