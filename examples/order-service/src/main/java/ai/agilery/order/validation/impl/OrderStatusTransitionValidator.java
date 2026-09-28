package ai.agilery.order.validation.impl;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.OrderStatus;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.validation.ValidationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validation handler to ensure that any requested {@link OrderStatus} transition
 * for an existing order is valid according to predefined business rules.
 * Invalid transitions will result in a validation error.
 */
@Slf4j
@RequiredArgsConstructor
public class OrderStatusTransitionValidator implements ValidationHandler<OrderDto> {

    /**
     * Repository for accessing Order entities to retrieve current status.
     */
    private final OrderRepository orderRepository;

    /**
     * Validates the requested order status transition.
     *
     * @param orderDto The {@link OrderDto} containing the new status and the order ID.
     * @return A list of {@link ValidationErrorDto} if the transition is invalid,
     *         otherwise an empty list.
     */
    @Override
    public List<ValidationErrorDto> validate(OrderDto orderDto) {
        List<ValidationErrorDto> errors = new ArrayList<>();

        if (orderDto.getId() == null || orderDto.getOrderStatus() == null) {
            // Basic syntactic validation should handle null ID/status.
            // This handler focuses on the transition logic.
            return errors;
        }

        Optional<ai.agilery.order.entity.Order> existingOrderOpt = orderRepository.findById(orderDto.getId());

        if (existingOrderOpt.isEmpty()) {
            // Order existence should be checked by a prior validator in the chain.
            // If it's not found here, it means a previous validator failed or was skipped.
            return errors;
        }

        // Get the entity's OrderStatus and convert it to the DTO/enumeration OrderStatus for comparison
        ai.agilery.order.entity.OrderStatus entityCurrentStatus = existingOrderOpt.get().getOrderStatus();
        OrderStatus currentStatus = OrderStatus.valueOf(entityCurrentStatus.name());
        OrderStatus newStatus = OrderStatus.valueOf(orderDto.getOrderStatus()); // This is already the DTO/enumeration OrderStatus

        if (currentStatus.equals(newStatus)) {
            // No status change, so no transition to validate.
            return errors;
        }

        if (!isValidTransition(currentStatus, newStatus)) {
            log.warn("Validation failed: Invalid order status transition from {} to {} for Order ID {}.",
                    currentStatus, newStatus, orderDto.getId());
            errors.add(buildError("orderDto", "orderStatus",
                    String.format("Invalid status transition from \'%s\' to \'%s\'.", currentStatus, newStatus)));
        }

        return errors;
    }

    /**
     * Determines if a given order status transition is valid.
     *
     * @param currentStatus The current status of the order (DTO/enumeration OrderStatus).
     * @param newStatus     The desired new status for the order (DTO/enumeration OrderStatus).
     * @return True if the transition is valid, false otherwise.
     */
    private boolean isValidTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        return switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED -> false; // Cannot change status after delivery
            case CANCELLED -> false; // Cannot change status after cancellation
            // Default case for any unhandled or future statuses, ensuring explicit handling
            default -> false;
        };
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
