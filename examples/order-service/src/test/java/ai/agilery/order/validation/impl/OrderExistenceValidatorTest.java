package ai.agilery.order.validation.impl;

import java.util.UUID;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.entity.Order;
import ai.agilery.order.repository.OrderRepository;

/**
 * Unit tests for {@link OrderExistenceValidator}.
 *
 * <p>
 * Verifies order existence validation logic for both modes: order must exist
 * and order must not exist.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OrderExistenceValidatorTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderExistenceValidator<UUID> mustExistValidator;
    private OrderExistenceValidator<UUID> mustNotExistValidator;

    @BeforeEach
    void setUp() {
        mustExistValidator = new OrderExistenceValidator<>(orderRepository, true);
        mustNotExistValidator = new OrderExistenceValidator<>(orderRepository, false);
    }

    /**
     * Verifies that validation succeeds when an order is required to exist and the
     * order is found in the repository.
     *
     * <p>
     * In this case, no validation errors should be returned.
     * </p>
     */
    @Test
    void testForValidationSuccessWhenOrderMustExistAndFound() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Order order = new Order();
        order.setId(orderId);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = mustExistValidator.validate(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertTrue(errors.isEmpty(), "Expected no validation errors when order exists");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
    }

    /**
     * Verifies that validation success when an order is required to not exist and
     * no order is found for the given ID.
     *
     * <p>
     * In this case, the absence of the order is valid and no validation errors
     * should be returned.
     * </p>
     */
    @Test
    void testForValidationSuccessWhenOrderMustNotExistAndNotFound() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = mustNotExistValidator.validate(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertTrue(errors.isEmpty(), "Expected no validation errors when order does not exist");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
    }

    /**
     * Verifies that validation fails when the order ID is {@code null} and the
     * order is required to exist.
     * <p>
     * A missing order ID must result in a validation error and the repository
     * should not be called.
     * </p>
     */
    @Test
    void testForValidationFailureWhenOrderIdIsNullAndMustExist() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = null;

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = mustExistValidator.validate(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(1, errors.size());

        ValidationErrorDto error = errors.get(0);
        Assertions.assertEquals("orderDto", error.getObjectName());
        Assertions.assertEquals("id", error.getField());
        Assertions.assertEquals("Order ID cannot be null for this operation.", error.getMessage());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verifyNoInteractions(orderRepository);
    }

    /**
     * Verifies that validation fails when an order is required to exist but no
     * order is found for the given ID.
     *
     * <p>
     * In this case, a validation error should be returned indicating that the order
     * does not exist with this id.
     * </p>
     */
    @Test
    void testForValidationFailureWhenOrderMustExistButNotFound() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = mustExistValidator.validate(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(1, errors.size());

        ValidationErrorDto error = errors.get(0);
        Assertions.assertEquals("orderDto", error.getObjectName());
        Assertions.assertEquals("id", error.getField());
        Assertions.assertEquals("Order with ID '00000000-0000-0000-0000-000000000001' not found.", error.getMessage());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
    }

    /**
     * Verifies that validation fails when an order is required to not exist but an
     * order with the given ID is already present.
     *
     * <p>
     * In this scenario, a validation error should be returned indicating that the
     * order already exists.
     * </p>
     */
    @Test
    void testForValidationFailureWhenOrderMustNotExistButAlreadyExists() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Order order = new Order();
        order.setId(orderId);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = mustNotExistValidator.validate(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(1, errors.size());

        ValidationErrorDto error = errors.get(0);
        Assertions.assertEquals("orderDto", error.getObjectName());
        Assertions.assertEquals("id", error.getField());
        Assertions.assertEquals("Order with ID '00000000-0000-0000-0000-000000000001' already exists.", error.getMessage());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
    }
}
