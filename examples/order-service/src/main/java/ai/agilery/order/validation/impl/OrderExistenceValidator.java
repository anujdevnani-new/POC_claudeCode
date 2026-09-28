package ai.agilery.order.validation.impl;

import ai.agilery.order.dto.OrderDto;
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
 * Validation handler to check the existence of an Order by its ID.
 * This handler can be configured to assert either that an order *must* exist
 * or that it *must not* exist, depending on the use case (e.g., update vs. create).
 *
 * @param <T> The type of object being validated, which can be OrderDto or UUID (for ID).
 */
@Slf4j
@RequiredArgsConstructor
public class OrderExistenceValidator<T> implements ValidationHandler<T> {

    /**
     * Repository for accessing Order entities.
     */
    private final OrderRepository orderRepository;

    /**
     * Flag indicating whether the order must exist (true) or must not exist (false).
     */
    private final boolean mustExist;

    /**
     * Validates the existence of an order based on the provided input.
     *
     * @param data The data to validate, which can be an OrderDto (containing ID) or a UUID (the ID itself).
     * @return A list of {@link ValidationErrorDto} if validation fails, otherwise an empty list.
     */
    @Override
    public List<ValidationErrorDto> validate(T data) {
        List<ValidationErrorDto> errors = new ArrayList<>();
        UUID orderId = extractOrderId(data);

        if (orderId == null) {
            // If ID is null, and we expect it to exist, it's an error.
            // If we expect it not to exist (e.g., for create), null ID is fine.
            if (mustExist) {
                errors.add(buildError("orderDto", "id", "Order ID cannot be null for this operation."));
            }
            return errors;
        }

        Optional<ai.agilery.order.entity.Order> existingOrder = orderRepository.findById(orderId);

        if (mustExist && existingOrder.isEmpty()) {
            log.warn("Validation failed: Order with ID {} not found.", orderId);
            errors.add(buildError("orderDto", "id", String.format("Order with ID '%s' not found.", orderId)));
        } else if (!mustExist && existingOrder.isPresent()) {
            log.warn("Validation failed: Order with ID {} already exists.", orderId);
            errors.add(buildError("orderDto", "id", String.format("Order with ID '%s' already exists.", orderId)));
        }

        return errors;
    }

    /**
     * Extracts the order ID from the input data.
     *
     * @param data The input data, which can be an OrderDto or a UUID.
     * @return The extracted Order ID, or null if not found or invalid type.
     */
    private UUID extractOrderId(T data) {
        if (data instanceof OrderDto) {
            return ((OrderDto) data).getId();
        } else if (data instanceof UUID) {
            return (UUID) data;
        }
        return null;
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
