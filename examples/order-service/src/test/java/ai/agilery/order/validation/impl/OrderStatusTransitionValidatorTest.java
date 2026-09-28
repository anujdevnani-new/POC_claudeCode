package ai.agilery.order.validation.impl;

import java.util.UUID;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.OrderStatus;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.entity.Order;
import ai.agilery.order.repository.OrderRepository;

/**
 * Unit tests for {@link OrderStatusTransitionValidator}.
 *
 * <p>
 * Verifies that order status transitions are validated correctly according to
 * defined business rules and constraints.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OrderStatusTransitionValidatorTest {

    @Mock
    private OrderRepository orderRepository;

    /**
     * Verifies that validation succeeds when the requested order status is the same
     * as the current status.
     * <p>
     * Since no actual status transition occurs, no validation errors should be
     * returned.
     * </p>
     */
    @Test
    void testForValidationSuccessWhenStatusIsSame() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        OrderStatusTransitionValidator validator = new OrderStatusTransitionValidator(orderRepository);

        OrderDto orderDto = new OrderDto();
        orderDto.setId(orderId);
        orderDto.setOrderStatus(OrderStatus.PENDING.name());

        Order order = new Order();
        order.setId(orderId);
        order.setOrderStatus(ai.agilery.order.entity.OrderStatus.PENDING);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertTrue(errors.isEmpty(), "Expected no validation errors when status does not change");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
    }

    /**
     * Verifies that validation succeeds when the requested order status transition
     * is valid according to business rules.
     * <p>
     * A valid transition (e.g., PENDING → CONFIRMED) should not produce any
     * validation errors.
     * </p>
     */
    @Test
    void testForValidationSuccessWhenValidStatusTransition() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        OrderStatusTransitionValidator validator = new OrderStatusTransitionValidator(orderRepository);

        OrderDto orderDto = new OrderDto();
        orderDto.setId(orderId);
        orderDto.setOrderStatus(OrderStatus.CONFIRMED.name());

        Order order = new Order();
        order.setId(orderId);
        order.setOrderStatus(ai.agilery.order.entity.OrderStatus.PENDING);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertTrue(errors.isEmpty(), "Expected no validation errors for valid status transition");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
    }

    /**
     * Verifies that validation fails when an invalid order status transition is
     * requested.
     * <p>
     * A transition from DELIVERED to CANCELLED is not allowed and must result in a
     * validation error.
     * </p>
     */
    @Test
    void testForValidationFailureWhenInvalidStatusTransition() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        OrderStatusTransitionValidator validator = new OrderStatusTransitionValidator(orderRepository);

        OrderDto orderDto = new OrderDto();
        orderDto.setId(orderId);
        orderDto.setOrderStatus(OrderStatus.CANCELLED.name());

        Order order = new Order();
        order.setId(orderId);
        order.setOrderStatus(ai.agilery.order.entity.OrderStatus.DELIVERED);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

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
        Assertions.assertEquals("orderStatus", error.getField());
        Assertions.assertEquals("Invalid status transition from 'DELIVERED' to 'CANCELLED'.", error.getMessage());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
    }
}
