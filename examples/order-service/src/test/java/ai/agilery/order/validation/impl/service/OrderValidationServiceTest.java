package ai.agilery.order.validation.impl.service;

import java.util.UUID;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.exceptions.ResourceNotFoundException;
import ai.agilery.order.exceptions.ValidationException;
import ai.agilery.order.utils.OrderTestHelper;
import ai.agilery.order.validation.impl.chain.OrderCreateValidationChain;
import ai.agilery.order.validation.impl.chain.OrderDeleteValidationChain;
import ai.agilery.order.validation.impl.chain.OrderGetAllValidationChain;
import ai.agilery.order.validation.impl.chain.OrderGetValidationChain;
import ai.agilery.order.validation.impl.chain.OrderPatchValidationChain;
import ai.agilery.order.validation.impl.chain.OrderUpdateValidationChain;
import ai.agilery.order.validation.util.CommonValidationUtil;

/**
 * Unit tests for {@link OrderValidationService}.
 *
 * <p>
 * Verifies correct validation orchestration, exception delegation, and behavior
 * for both success and failure scenarios.
 */
@ExtendWith(MockitoExtension.class)
class OrderValidationServiceTest {

    @Mock
    private OrderCreateValidationChain orderCreateValidationChain;

    @Mock
    private OrderUpdateValidationChain orderUpdateValidationChain;

    @Mock
    private OrderPatchValidationChain orderPatchValidationChain;

    @Mock
    private OrderGetValidationChain orderGetValidationChain;

    @Mock
    private OrderGetAllValidationChain orderGetAllValidationChain;

    @Mock
    private OrderDeleteValidationChain orderDeleteValidationChain;

    @Mock
    private CommonValidationUtil commonValidationUtil;

    @InjectMocks
    private OrderValidationService orderValidationService;

    private final ValidationErrorDto validationErrorDto = new ValidationErrorDto() {
        {
            setField("dummyField");
            setMessage("dummyMessage");
        }
    };

    /**
     * Verifies that order creation succeeds when no validation errors are returned
     * by the {@link OrderCreateValidationChain}.
     */
    @Test
    void testForCreateWithValidationSuccess() {
        OrderDto dto = OrderTestHelper.createOrderDto();

        Mockito.when(orderCreateValidationChain.validate(dto)).thenReturn(Collections.emptyList());
        Assertions.assertDoesNotThrow(() -> orderValidationService.onCreate(dto));
        Mockito.verify(orderCreateValidationChain, Mockito.times(1)).validate(dto);
    }

    /**
     * Verifies that retrieving an order by ID succeeds when the order exists and no
     * validation errors are returned.
     */
    @Test
    void testGetWithValidationSuccess() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Mockito.when(orderGetValidationChain.validate(id)).thenReturn(Collections.emptyList());
        Assertions.assertDoesNotThrow(() -> orderValidationService.onGet(id));
        Mockito.verify(orderGetValidationChain, Mockito.times(1)).validate(id);
    }

    /**
     * Verifies that retrieving all orders succeeds when pagination parameters are
     * valid.
     */
    @Test
    void testGetAllWithValidationSuccess() {
        Pageable pageable = PageRequest.of(0, 10);

        Mockito.when(orderGetAllValidationChain.validate(pageable)).thenReturn(Collections.emptyList());
        Assertions.assertDoesNotThrow(() -> orderValidationService.onGetAll(pageable));
        Mockito.verify(orderGetAllValidationChain, Mockito.times(1)).validate(pageable);
    }

    /**
     * Verifies that partial update (patch) succeeds when no validation errors are
     * returned.
     */
    @Test
    void testPatchWithValidationSuccess() {
        OrderDto dto = OrderTestHelper.createOrderDto();

        Mockito.when(orderPatchValidationChain.validate(dto)).thenReturn(Collections.emptyList());
        Assertions.assertDoesNotThrow(() -> orderValidationService.onPatch(dto));
        Mockito.verify(orderPatchValidationChain, Mockito.times(1)).validate(dto);
    }

    /**
     * Verifies that deleting an order succeeds when validation passes.
     */
    @Test
    void testDeleteWithValidationSuccess() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Mockito.when(orderDeleteValidationChain.validate(id)).thenReturn(Collections.emptyList());
        Assertions.assertDoesNotThrow(() -> orderValidationService.onDelete(id));
        Mockito.verify(orderDeleteValidationChain, Mockito.times(1)).validate(id);
    }

    /**
     * Verifies that updating an order succeeds when validation passes.
     */
    @Test
    void testUpdateWithValidationSuccess() {
        OrderDto dto = OrderTestHelper.createOrderDto();

        Mockito.when(orderUpdateValidationChain.validate(dto)).thenReturn(Collections.emptyList());
        Assertions.assertDoesNotThrow(() -> orderValidationService.onUpdate(dto));
        Mockito.verify(orderUpdateValidationChain, Mockito.times(1)).validate(dto);
    }

    /**
     * Verifies that order creation fails with {@link ValidationException} when
     * validation errors are present.
     */
    @Test
    void testCreateWithValidationFailures() {
        OrderDto dto = OrderTestHelper.createOrderDto();

        Mockito.when(orderCreateValidationChain.validate(dto)).thenReturn(List.of(validationErrorDto));

        Mockito.doThrow(new ValidationException(List.of(validationErrorDto))).when(commonValidationUtil)
                .throwValidationException(List.of(validationErrorDto));

        Assertions.assertThrows(ValidationException.class, () -> orderValidationService.onCreate(dto));

        Mockito.verify(orderCreateValidationChain, Mockito.times(1)).validate(dto);
        Mockito.verify(commonValidationUtil, Mockito.times(1)).throwValidationException(List.of(validationErrorDto));
    }

    /**
     * Verifies that updating an order fails with {@link ValidationException} when
     * validation errors are present.
     */
    @Test
    void testUpdateWithValidationFailures() {
        OrderDto dto = OrderTestHelper.createOrderDto();

        Mockito.when(orderUpdateValidationChain.validate(dto)).thenReturn(List.of(validationErrorDto));

        Mockito.doThrow(new ValidationException(List.of(validationErrorDto))).when(commonValidationUtil)
                .throwValidationException(List.of(validationErrorDto));

        Assertions.assertThrows(ValidationException.class, () -> orderValidationService.onUpdate(dto));
        Mockito.verify(orderUpdateValidationChain, Mockito.times(1)).validate(dto);
        Mockito.verify(commonValidationUtil, Mockito.times(1)).throwValidationException(List.of(validationErrorDto));
    }

    /**
     * Verifies that patching an order fails with {@link ValidationException} when
     * validation errors are present.
     */
    @Test
    void testPatchWithValidationFailures() {
        OrderDto dto = OrderTestHelper.createOrderDto();

        Mockito.when(orderPatchValidationChain.validate(dto)).thenReturn(List.of(validationErrorDto));

        Mockito.doThrow(new ValidationException(List.of(validationErrorDto))).when(commonValidationUtil)
                .throwValidationException(List.of(validationErrorDto));

        Assertions.assertThrows(ValidationException.class, () -> orderValidationService.onPatch(dto));
        Mockito.verify(orderPatchValidationChain, Mockito.times(1)).validate(dto);
        Mockito.verify(commonValidationUtil, Mockito.times(1)).throwValidationException(List.of(validationErrorDto));
    }

    /**
     * Verifies that retrieving an order by ID fails with
     * {@link ResourceNotFoundException} when the order does not exist.
     */
    @Test
    void testGetWithValidationFailures() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Mockito.when(orderGetValidationChain.validate(id)).thenReturn(List.of(validationErrorDto));

        Mockito.doThrow(new ResourceNotFoundException("Order with '00000000-0000-0000-0000-000000000001' does not exist")).when(commonValidationUtil)
                .throwResourceNotFoundException(List.of(validationErrorDto), "Order with '00000000-0000-0000-0000-000000000001' does not exist");

        Assertions.assertThrows(ResourceNotFoundException.class, () -> orderValidationService.onGet(id));
        Mockito.verify(orderGetValidationChain, Mockito.times(1)).validate(id);
        Mockito.verify(commonValidationUtil, Mockito.times(1))
                .throwResourceNotFoundException(List.of(validationErrorDto), "Order with '00000000-0000-0000-0000-000000000001' does not exist");
    }

    /**
     * Verifies that retrieving all orders fails with {@link ValidationException}
     * when pagination or sorting parameters are invalid.
     */
    @Test
    void testGetAllWithValidationFailures() {
        Pageable pageable = PageRequest.of(0, 10);

        Mockito.when(orderGetAllValidationChain.validate(pageable)).thenReturn(List.of(validationErrorDto));

        Mockito.doThrow(new ValidationException(List.of(validationErrorDto))).when(commonValidationUtil)
                .throwValidationException(List.of(validationErrorDto));

        Assertions.assertThrows(ValidationException.class, () -> orderValidationService.onGetAll(pageable));
        Mockito.verify(orderGetAllValidationChain, Mockito.times(1)).validate(pageable);
        Mockito.verify(commonValidationUtil, Mockito.times(1)).throwValidationException(List.of(validationErrorDto));
    }

    /**
     * Verifies that deleting an order fails with {@link ValidationException} when
     * validation errors are present.
     */
    @Test
    void testDeleteWithValidationFailures() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Mockito.when(orderDeleteValidationChain.validate(id)).thenReturn(List.of(validationErrorDto));

        Mockito.doThrow(new ValidationException(List.of(validationErrorDto))).when(commonValidationUtil)
                .throwValidationException(List.of(validationErrorDto));

        Assertions.assertThrows(ValidationException.class, () -> orderValidationService.onDelete(id));
        Mockito.verify(orderDeleteValidationChain, Mockito.times(1)).validate(id);
        Mockito.verify(commonValidationUtil, Mockito.times(1)).throwValidationException(List.of(validationErrorDto));
    }

}
