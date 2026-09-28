package ai.agilery.order.validation.impl.chain;

import org.springframework.stereotype.Component;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.validation.AbstractValidationChain;
import ai.agilery.order.validation.impl.OrderHasItemsValidator;
import ai.agilery.order.validation.impl.OrderTotalAmountMatchValidator;
import lombok.extern.slf4j.Slf4j;

/**
 * Validation chain for creating a new {@link OrderDto}.
 * This chain ensures that:
 * <ul>
 *     <li>The order contains at least one item.</li>
 *     <li>The total amount of the order matches the sum of its item subtotals.</li>
 * </ul>
 */
@Component
@Slf4j
public class OrderCreateValidationChain extends AbstractValidationChain<OrderDto> {

    /**
     * Initializes the validation chain with handlers specific to order creation.
     * This method is called automatically after dependency injection.
     */

    public OrderCreateValidationChain() {
        log.info("Initializing OrderCreateValidationChain...");
        this.add(new OrderHasItemsValidator());
        this.add(new OrderTotalAmountMatchValidator());
        log.info("OrderCreateValidationChain initialized...");
    }
}
