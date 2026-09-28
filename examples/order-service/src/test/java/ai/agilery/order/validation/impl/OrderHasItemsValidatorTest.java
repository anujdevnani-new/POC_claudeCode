package ai.agilery.order.validation.impl;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.OrderItemDto;
import ai.agilery.order.dto.ValidationErrorDto;

/**
 * Unit tests for {@link OrderHasItemsValidator}.
 *
 * <p>
 * Verifies that an order must contain at least one order item.
 * </p>
 */
class OrderHasItemsValidatorTest {

    private final OrderHasItemsValidator validator = new OrderHasItemsValidator();

    /**
     * Verifies that validation success when the order contains at least one order
     * item.
     * <p>
     * An order with one or more items is considered valid and should not produce
     * any validation errors.
     * </p>
     */
    @Test
    void testForValidationSuccessWhenOrderHasItems() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderItems(List.of(new OrderItemDto()));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertTrue(errors.isEmpty(), "Expected no validation errors when order has at least one item");
    }

    /**
     * Verifies that validation fails when the order items list is {@code null}.
     * <p>
     * An order without any items is invalid and must result in a validation error.
     * </p>
     */
    @Test
    void testForValidationFailureWhenOrderItemsIsNull() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderItems(null);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(1, errors.size());

        ValidationErrorDto error = errors.get(0);
        Assertions.assertEquals("orderDto", error.getObjectName());
        Assertions.assertEquals("orderItems", error.getField());
        Assertions.assertEquals("Order must contain at least one item.", error.getMessage());
    }

    /**
     * Verifies that validation fails when the order items list is empty.
     * <p>
     * An order with no items is considered invalid and must result in
     * a validation error.
     * </p>
     */
    @Test
    void testForValidationFailureWhenOrderItemsIsEmpty() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderItems(Collections.emptyList());

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(1, errors.size());

        ValidationErrorDto error = errors.get(0);
        Assertions.assertEquals("orderDto", error.getObjectName());
        Assertions.assertEquals("orderItems", error.getField());
        Assertions.assertEquals("Order must contain at least one item.", error.getMessage());
    }

}
