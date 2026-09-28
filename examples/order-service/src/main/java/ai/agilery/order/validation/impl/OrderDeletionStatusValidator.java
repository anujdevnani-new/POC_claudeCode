package ai.agilery.order.validation.impl;

import ai.agilery.order.dto.OrderStatus;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.validation.ValidationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Validation handler to ensure that an order can only be "deleted" (soft-deleted by setting status to CANCELLED)
 * if its current status is either PENDING or CONFIRMED.
 * This prevents cancellation of orders that are already shipped or delivered.
 */
@Slf4j
@RequiredArgsConstructor
public class OrderDeletionStatusValidator implements ValidationHandler<UUID> {

    /**
     * Repository for accessing Order entities to retrieve current status.
     */
    private final OrderRepository orderRepository;

    /**
     * Validates if an order can be deleted based on its current status.
     *
     * @param orderId The ID of the order to validate for deletion.
     * @return A list of {@link ValidationErrorDto} if the order cannot be deleted due to its status,
     *         otherwise an empty list.
     */
    @Override
    public List<ValidationErrorDto> validate(UUID orderId) {
        List<ValidationErrorDto> errors = new ArrayList<>();

        if (orderId == null) {
            errors.add(buildError("orderId", "id", "Order ID cannot be null for deletion."));
            return errors;
        }

        Optional<ai.agilery.order.entity.Order> existingOrderOpt = orderRepository.findById(orderId);

        if (existingOrderOpt.isEmpty()) {
            // Order existence should be checked by a prior validator in the chain.
            // If it's not found here, it means a previous validator failed or was skipped.
            return errors;
        }

        // Get the entity's OrderStatus and convert it to the DTO/enumeration OrderStatus for comparison
        ai.agilery.order.entity.OrderStatus entityCurrentStatus = existingOrderOpt.get().getOrderStatus();
        OrderStatus currentStatus = OrderStatus.valueOf(entityCurrentStatus.name());

        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            log.warn("Validation failed: Order with ID {} cannot be cancelled because its status is {}.",
                    orderId, currentStatus);
            errors.add(buildError("orderId", "orderStatus",
                    String.format("Order cannot be cancelled in \'%s\' status. Only PENDING or CONFIRMED orders can be cancelled.", currentStatus)));
        }

        return errors;
    }

    /**
     * Builds a {@link ValidationErrorDto} for the given object, field, and message.
     *
     * @param objectName The name of the object being validated.
     * @param field      The field with the error.
     * @param message    The error message.
     * @return A populated ValidationErrorDto.
     */
    private ValidationErrorDto buildError(String objectName, String field, String message) {
        ValidationErrorDto error = new ValidationErrorDto();
        error.setObjectName(objectName);
        error.setField(field);
        error.setMessage(message);
        return error;
    }
}
