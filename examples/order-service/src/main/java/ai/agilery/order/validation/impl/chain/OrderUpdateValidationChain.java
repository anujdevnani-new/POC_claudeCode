package ai.agilery.order.validation.impl.chain;

import org.springframework.stereotype.Component;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.validation.AbstractValidationChain;
import ai.agilery.order.validation.impl.OrderExistenceValidator;
import ai.agilery.order.validation.impl.OrderTotalAmountMatchValidator;
import lombok.extern.slf4j.Slf4j;

/**
 * Validation chain for updating an existing {@link OrderDto}. This chain
 * ensures that:
 * <ul>
 * <li>The order to be updated actually exists.</li>
 * <li>The total amount of the order matches the sum of its item subtotals (if
 * items are part of the update, though requirements state "not items").
 * Assuming total amount can be updated and should still match.</li>
 * </ul>
 */
@Component
@Slf4j
public class OrderUpdateValidationChain extends AbstractValidationChain<OrderDto> {

    /**
     * Constructs an OrderUpdateValidationChain with the necessary repository.
     *
     * @param orderRepository The repository for order data access.
     */
    public OrderUpdateValidationChain(OrderRepository orderRepository) {
        log.info("Initializing OrderUpdateValidationChain...");
        this.add(new OrderExistenceValidator<>(orderRepository, true)); // Order must exist
        this.add(new OrderTotalAmountMatchValidator()); // Re-verify total amount consistency
        log.info("OrderUpdateValidationChain initialized");
    }
}
