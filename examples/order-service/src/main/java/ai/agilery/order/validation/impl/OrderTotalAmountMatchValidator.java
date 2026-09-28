package ai.agilery.order.validation.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.validation.ValidationHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Validation handler to verify that the total amount of an {@link ai.agilery.order.dto.OrderDto}
 * accurately reflects the sum of its individual {@link ai.agilery.order.dto.OrderItemDto} subtotals.
 * This ensures data consistency and prevents discrepancies in order pricing.
 */
@Slf4j
public class OrderTotalAmountMatchValidator implements ValidationHandler<OrderDto> {

    /**
     * Validates that the {@code totalAmount} in the {@link OrderDto} matches the sum
     * of the {@code subtotal}s of all {@link OrderItemDto}s within the order.
     *
     * @param orderDto The {@link OrderDto} to validate.
     * @return A list of {@link ValidationErrorDto} if the total amount does not match,
     *         otherwise an empty list.
     */
    @Override
    public List<ValidationErrorDto> validate(OrderDto orderDto) {
        List<ValidationErrorDto> errors = new ArrayList<>();

        if (orderDto.getOrderItems() == null || orderDto.getTotalAmount() == null) {
            // Syntactic validation should catch nulls for totalAmount and empty orderItems.
            // This semantic validator assumes basic structure is present.
            return errors;
        }

        BigDecimal calculatedTotal = orderDto.getOrderItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (orderDto.getTotalAmount().compareTo(calculatedTotal) != 0) {
            log.warn("Validation failed: Order total amount ({}) does not match sum of item subtotals ({}).",
                    orderDto.getTotalAmount(), calculatedTotal);
            errors.add(buildError("orderDto", "totalAmount",
                    String.format("Total amount (%s) does not match the sum of order item subtotals (%s).",
                            orderDto.getTotalAmount(), calculatedTotal)));
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
