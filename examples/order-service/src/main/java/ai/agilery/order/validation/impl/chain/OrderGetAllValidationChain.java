package ai.agilery.order.validation.impl.chain;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import ai.agilery.order.validation.AbstractValidationChain;
import ai.agilery.order.validation.impl.PaginationParameterValidator;
import lombok.extern.slf4j.Slf4j;

/**
 * Validation chain for retrieving all orders with pagination and sorting.
 * This chain ensures that:
 * <ul>
 *     <li>Sorting parameters are applied only to allowed fields of the {@link ai.agilery.order.dto.OrderDto}.</li>
 * </ul>
 */
@Component
@Slf4j
public class OrderGetAllValidationChain extends AbstractValidationChain<Pageable> {

    /**
     * Set of allowed fields for sorting orders.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Stream.of(
            "id", "customerName", "customerEmail", "orderDate", "orderStatus", "totalAmount",
            "paymentMethod", "createdDateTime", "lastModifiedDateTime"
            ).collect(Collectors.toSet());

    /**
     * Initializes the validation chain with handlers specific to retrieving all orders.
     * This method is called automatically after dependency injection.
     */
    public OrderGetAllValidationChain() {
        log.info("Initializing OrderGetAllValidationChain...");
        this.add(new PaginationParameterValidator(ALLOWED_SORT_FIELDS, "orderDto"));
        log.info("OrderGetAllValidationChain initialized");
    }
}
