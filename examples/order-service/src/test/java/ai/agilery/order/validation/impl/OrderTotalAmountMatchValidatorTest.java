package ai.agilery.order.validation.impl;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.OrderItemDto;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.utils.OrderItemTestHelper;

/**
 * Unit tests for {@link OrderTotalAmountMatchValidator}.
 *
 * <p>
 * Verifies that the order total amount matches the sum of all order item
 * subtotals.
 * </p>
 */
class OrderTotalAmountMatchValidatorTest {

    /**
     * Verifies that validation succeeds when the total amount matches the sum of
     * order item subtotals.
     */
    @Test
    void testForValidationSuccess() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderTotalAmountMatchValidator validator = new OrderTotalAmountMatchValidator();

        OrderItemDto item1 = new OrderItemDto();
        item1.setUnitPrice(new BigDecimal("100.00"));
        item1.setQuantity(1);

        OrderItemDto item2 = new OrderItemDto();
        item2.setUnitPrice(new BigDecimal("50.00"));
        item2.setQuantity(2);

        OrderDto orderDto = new OrderDto();
        orderDto.setOrderItems(List.of(item1, item2));
        orderDto.setTotalAmount(new BigDecimal("200.00"));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertTrue(errors.isEmpty(), "Expected no validation errors when total amount matches item sum");
    }

    /**
     * Verifies that validation fails when the total amount does not match the sum
     * of order item subtotals.
     */
    @Test
    void testForValidationFailure() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderTotalAmountMatchValidator validator = new OrderTotalAmountMatchValidator();

        OrderItemDto item1 = OrderItemTestHelper.createOrderItemDto();
        OrderItemDto item2 = OrderItemTestHelper.createOrderItemDto();

        OrderDto orderDto = new OrderDto();
        orderDto.setOrderItems(List.of(item1, item2));
        orderDto.setTotalAmount(new BigDecimal("2400.00"));

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
        Assertions.assertEquals("totalAmount", error.getField());
        Assertions.assertEquals("Total amount (2400.00) does not match the sum of order item subtotals (4800.00).",
                error.getMessage());
    }
}
