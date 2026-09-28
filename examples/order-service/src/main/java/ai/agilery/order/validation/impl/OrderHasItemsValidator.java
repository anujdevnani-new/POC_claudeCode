package ai.agilery.order.validation.impl;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.validation.ValidationHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation handler to ensure that an {@link OrderDto} contains at least one order item.
 * This is a semantic validation rule for order creation.
 */
@Slf4j
public class OrderHasItemsValidator implements ValidationHandler<OrderDto> {

    /**
     * Validates that the {@link OrderDto} has a non-empty list of order items.
     *
     * @param orderDto The {@link OrderDto} to validate.
     * @return A list of {@link ValidationErrorDto} if the order items list is empty or null,
     *         otherwise an empty list.
     */
    @Override
    public List<ValidationErrorDto> validate(OrderDto orderDto) {
        List<ValidationErrorDto> errors = new ArrayList<>();

        if (orderDto.getOrderItems() == null || orderDto.getOrderItems().isEmpty()) {
            log.warn("Validation failed: Order must contain at least one item.");
            errors.add(buildError("orderDto", "orderItems", "Order must contain at least one item."));
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
