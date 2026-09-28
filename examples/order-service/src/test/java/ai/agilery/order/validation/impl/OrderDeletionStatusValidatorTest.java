package ai.agilery.order.validation.impl;

import java.util.UUID;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.entity.Order;
import ai.agilery.order.repository.OrderRepository;

/**
 * Unit tests for {@link OrderDeletionStatusValidator}.
 *
 * <p>
 * Verifies that an order can be cancelled only when its status is
 * {@code PENDING} or {@code CONFIRMED}. Any other status must result in a
 * validation error.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OrderDeletionStatusValidatorTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderDeletionStatusValidator validator;

    /**
     * Verifies that validation succeeds when the order status is {@code PENDING}.
     * <p>
     * Orders in {@code PENDING} status are allowed to be cancelled, so no
     * validation errors should be returned.
     * </p>
     */
    @Test
    void testForValidationSuccessWhenOrderStatusIsPending() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Order order = new Order();
        order.setId(orderId);
        order.setOrderStatus(ai.agilery.order.entity.OrderStatus.PENDING);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertTrue(errors.isEmpty(), "Expected no validation errors for PENDING order");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
    }

    /**
     * Verifies that validation fails when the order ID is {@code null}.
     *
     * <p>
     * A null order ID is invalid for deletion and must result in a validation error
     * without calling the repository.
     * </p>
     */
    @Test
    void testForValidationFailureWhenOrderIdIsNull() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = null;

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(1, errors.size());

        ValidationErrorDto error = errors.get(0);
        Assertions.assertEquals("orderId", error.getObjectName());
        Assertions.assertEquals("id", error.getField());
        Assertions.assertEquals("Order ID cannot be null for deletion.", error.getMessage());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verifyNoInteractions(orderRepository);
    }

    /**
     * Verifies that validation fails when the order status is neither
     * {@code PENDING} nor {@code CONFIRMED}.
     *
     * <p>
     * Orders in any other status (for example {@code SHIPPED}) must not be allowed
     * for cancellation and should produce a validation error.
     * </p>
     */
    @Test
    void testForValidationFailureWhenOrderStatusIsNotPendingOrConfirmed() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Order order = new Order();
        order.setId(orderId);
        order.setOrderStatus(ai.agilery.order.entity.OrderStatus.SHIPPED);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(1, errors.size(),
                "Expected validation error when order status is not PENDING or CONFIRMED");

        ValidationErrorDto error = errors.get(0);
        Assertions.assertEquals("orderId", error.getObjectName());
        Assertions.assertEquals("orderStatus", error.getField());
        Assertions.assertEquals(
                "Order cannot be cancelled in 'SHIPPED' status. Only PENDING or CONFIRMED orders can be cancelled.",
                error.getMessage());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
    }

}
