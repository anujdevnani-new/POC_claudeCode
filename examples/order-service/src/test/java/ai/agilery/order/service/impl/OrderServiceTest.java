package ai.agilery.order.service.impl;

import java.util.UUID;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.OrderItemDto;
import ai.agilery.order.entity.Order;
import ai.agilery.order.entity.OrderItem;
import ai.agilery.order.entity.OrderStatus;
import ai.agilery.order.exceptions.ProcessingException;
import ai.agilery.order.exceptions.ResourceNotFoundException;
import ai.agilery.order.mapper.OrderItemMapper;
import ai.agilery.order.mapper.OrderMapper;
import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.utils.OrderItemTestHelper;
import ai.agilery.order.utils.OrderTestHelper;

/**
 * Unit tests for {@link OrderServiceImpl}.
 *
 * <p>
 * Verifies the business logic of order operations by mocking repository and
 * mapper dependencies. These tests focus on service-layer behavior without
 * loading the Spring context.
 * </p>
 */

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    /**
     * Tests successful creation of an order.
     *
     * <p>
     * Verifies that when a valid {@link OrderDto} is provided, the service
     * correctly maps the DTO to an entity, calculates the total amount, persists
     * the order, and returns the expected {@link OrderDto}.
     * </p>
     */
    @Test
    void testForCreate() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        Order order = OrderTestHelper.createOrderEntity();

        OrderItemDto itemDto = OrderItemTestHelper.createOrderItemDto();
        OrderItem orderItem = OrderItemTestHelper.createOrderItemEntity();

        ////////////////////////////////////////////////////////////////////////
        // 2. Mock behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderMapper.toEntity(orderDto)).thenReturn(order);
        Mockito.when(orderItemMapper.toEntity(itemDto)).thenReturn(orderItem);
        Mockito.when(orderRepository.save(order)).thenReturn(order);
        Mockito.when(orderMapper.toDto(order)).thenReturn(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method under test
        ////////////////////////////////////////////////////////////////////////
        OrderDto actual = orderService.create(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 4. VERIFY BUSINESS LOGIC
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(new BigDecimal("2400.00"), order.getTotalAmount(),
                "Total amount must be calculated from order items");
        Assertions.assertEquals(1, order.getOrderItems().size());
        Assertions.assertEquals(order, order.getOrderItems().get(0).getOrder(),
                "OrderItem must reference parent Order");
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(orderDto.toString(), actual.toString());

        ////////////////////////////////////////////////////////////////////////
        // 5. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderMapper).toEntity(orderDto);
        Mockito.verify(orderItemMapper).toEntity(itemDto);
        Mockito.verify(orderRepository).save(order);
        Mockito.verify(orderMapper).toDto(order);
    }

    /**
     * Tests updating an existing order.
     *
     * <p>
     * Verifies that when an order with a valid ID exists, the service correctly
     * updates the order details, maps and replaces order items, persists the
     * updated entity, and returns the expected {@link OrderDto}.
     * </p>
     */
    @Test
    void testForUpdate() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        Order existingOrder = OrderTestHelper.createOrderEntity();
        Assertions.assertEquals(1, existingOrder.getOrderItems().size());
        OrderItemDto newItemDto = orderDto.getOrderItems().get(0);
        OrderItem newOrderItem = OrderItemTestHelper.createOrderItemEntity();

        ////////////////////////////////////////////////////////////////////////
        // 2. Mock behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderRepository.findById(orderDto.getId())).thenReturn(Optional.of(existingOrder));
        Mockito.doNothing().when(orderMapper).update(existingOrder, orderDto);
        Mockito.when(orderItemMapper.toEntity(newItemDto)).thenReturn(newOrderItem);
        Mockito.when(orderRepository.save(existingOrder)).thenReturn(existingOrder);
        Mockito.when(orderMapper.toDto(existingOrder)).thenReturn(new OrderDto());

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method under test
        ////////////////////////////////////////////////////////////////////////
        OrderDto actual = orderService.update(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify BUSINESS LOGIC
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertNotNull(actual, "Updated OrderDto must not be null");
        Assertions.assertEquals(1, existingOrder.getOrderItems().size(), "Order items must be replaced with new items");
        OrderItem savedItem = existingOrder.getOrderItems().get(0);
        BigDecimal expectedTotal = orderDto.getTotalAmount();
        Assertions.assertEquals(expectedTotal, existingOrder.getTotalAmount(),
                "Total amount must be updated from DTO via mapper");

        Assertions.assertEquals(existingOrder, savedItem.getOrder(), "OrderItem must reference the updated Order");

        ////////////////////////////////////////////////////////////////////////
        // 5. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository).findById(orderDto.getId());
        Mockito.verify(orderMapper).update(existingOrder, orderDto);
        Mockito.verify(orderItemMapper).toEntity(newItemDto);
        Mockito.verify(orderRepository).save(existingOrder);
        Mockito.verify(orderMapper).toDto(existingOrder);
    }

    /**
     * Tests patching an existing order.
     *
     * <p>
     * Verifies that when an order with a valid ID exists, the service applies only
     * the non-null fields from the request, persists the updated entity, and
     * returns the expected {@link OrderDto}.
     * </p>
     */
    @Test
    void testForPatch() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto patchDto = OrderTestHelper.createOrderDtoForPatch();
        Order existingOrder = OrderTestHelper.createOrderEntity();

        OrderItemDto patchItemDto = OrderItemTestHelper.createOrderItemDto();
        patchDto.setOrderItems(Collections.singletonList(patchItemDto));

        Assertions.assertEquals(1, existingOrder.getOrderItems().size());

        OrderItem patchedItem = new OrderItem();
        patchedItem.setProductSku(patchItemDto.getProductSku());

        ////////////////////////////////////////////////////////////////////////
        // 2. Mock behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderRepository.findById(patchDto.getId())).thenReturn(Optional.of(existingOrder));
        Mockito.doNothing().when(orderMapper).patch(existingOrder, patchDto);
        Mockito.when(orderItemMapper.toEntity(patchItemDto)).thenReturn(patchedItem);
        Mockito.when(orderRepository.save(existingOrder)).thenReturn(existingOrder);
        Mockito.when(orderMapper.toDto(existingOrder)).thenReturn(patchDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method under test
        ////////////////////////////////////////////////////////////////////////
        OrderDto actual = orderService.patch(patchDto);

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify BUSINESS LOGIC
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(patchDto.getTotalAmount(), existingOrder.getTotalAmount(),
                "Only non-null fields from patch DTO must be applied");
        Assertions.assertEquals(1, existingOrder.getOrderItems().size(),
                "Old order items must be replaced during patch");
        OrderItem savedItem = existingOrder.getOrderItems().get(0);
        Assertions.assertEquals(existingOrder, savedItem.getOrder(),
                "Patched order item must be linked to existing order");
        Assertions.assertEquals(patchItemDto.getProductSku(), savedItem.getProductSku());

        ////////////////////////////////////////////////////////////////////////
        // 5. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository).findById(patchDto.getId());
        Mockito.verify(orderMapper).patch(existingOrder, patchDto);
        Mockito.verify(orderItemMapper).toEntity(patchItemDto);
        Mockito.verify(orderRepository).save(existingOrder);
        Mockito.verify(orderMapper).toDto(existingOrder);
    }

    /**
     * Tests retrieving an order by ID when it exists.
     *
     * <p>
     * Verifies that when a valid order ID is provided, the service retrieves the
     * corresponding entity, maps it to a {@link OrderDto}, and returns the expected
     * result.
     * </p>
     */
    @Test
    void testForGet() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Order existingOrder = OrderTestHelper.createOrderEntity();
        OrderDto expectedDto = OrderTestHelper.createOrderDto();

        Assertions.assertNotNull(existingOrder.getId());
        Assertions.assertFalse(existingOrder.getOrderItems().isEmpty());

        ////////////////////////////////////////////////////////////////////////
        // 2. Mock behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderRepository.findById(existingOrder.getId())).thenReturn(Optional.of(existingOrder));
        Mockito.when(orderMapper.toDto(existingOrder)).thenReturn(expectedDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method under test
        ////////////////////////////////////////////////////////////////////////
        OrderDto actual = orderService.get(existingOrder.getId());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify BUSINESS RESULT
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertNotNull(actual, "Returned OrderDto must not be null");
        Assertions.assertEquals(expectedDto.getCustomerName(), actual.getCustomerName(),
                "Customer name must match mapped entity");
        Assertions.assertEquals(expectedDto.getCustomerEmail(), actual.getCustomerEmail());
        Assertions.assertEquals(expectedDto.getDeliveryAddress(), actual.getDeliveryAddress());
        Assertions.assertEquals(expectedDto.getOrderStatus(), actual.getOrderStatus());
        Assertions.assertEquals(expectedDto.getPaymentMethod(), actual.getPaymentMethod());

        ////////////////////////////////////////////////////////////////////////
        // 5. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(existingOrder.getId());
        Mockito.verify(orderMapper, Mockito.times(1)).toDto(existingOrder);
        Mockito.verifyNoInteractions(orderItemMapper);
    }

    /**
     * Tests retrieving all orders with pagination.
     *
     * <p>
     * Verifies that the service returns a paginated list of orders, correctly maps
     * entities to {@link OrderDto}, and preserves pagination metadata.
     * </p>
     */
    @Test
    void testForGetAll() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Order order = OrderTestHelper.createOrderEntity();
        OrderDto dto = OrderTestHelper.createOrderDto();

        Pageable pageable = PageRequest.of(1, 2);
        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 5);

        ////////////////////////////////////////////////////////////////////////
        // 2. Mock behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderRepository.findAll(pageable)).thenReturn(orderPage);
        Mockito.when(orderMapper.toDto(order)).thenReturn(dto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method under test
        ////////////////////////////////////////////////////////////////////////
        Page<OrderDto> result = orderService.getAll(pageable);

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify BUSINESS LOGIC
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertNotNull(result, "Result page must not be null");

        Assertions.assertEquals(5, result.getTotalElements(), "Total elements must be preserved");
        Assertions.assertEquals(2, result.getSize(), "Page size must be preserved");
        Assertions.assertEquals(1, result.getNumber(), "Page number must be preserved");
        Assertions.assertEquals(1, result.getContent().size(), "Page content size must match");
        Assertions.assertEquals(dto.getCustomerEmail(), result.getContent().get(0).getCustomerEmail(),
                "Mapped DTO content must match entity");

        ////////////////////////////////////////////////////////////////////////
        // 5. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findAll(pageable);
        Mockito.verify(orderMapper, Mockito.times(1)).toDto(order);
    }

    /**
     * Tests soft deletion (cancellation) of an order.
     *
     * <p>
     * Verifies that when a valid order ID is provided, the service updates the
     * order status to {@code CANCELLED}, persists the change, and does not
     * physically delete the record.
     * </p>
     */
    @Test
    void testForDelete() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Order order = OrderTestHelper.createOrderEntity();
        OrderStatus originalStatus = order.getOrderStatus();
        UUID originalCreatedBy = order.getCreatedBy();
        UUID originalId = order.getId();

        ////////////////////////////////////////////////////////////////////////
        // 2. Mock behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        Mockito.when(orderRepository.save(order)).thenReturn(order);

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method under test
        ////////////////////////////////////////////////////////////////////////
        orderService.delete(order.getId());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify BUSINESS LOGIC
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(OrderStatus.CANCELLED, order.getOrderStatus(),
                "Order status must be changed to CANCELLED");
        Assertions.assertNotEquals(originalStatus, order.getOrderStatus(),
                "Order status must be modified during delete operation");
        Assertions.assertEquals(originalId, order.getId(), "Order ID must not change during soft delete");
        Assertions.assertEquals(originalCreatedBy, order.getCreatedBy(),
                "Audit fields must not be modified during soft delete");

        ////////////////////////////////////////////////////////////////////////
        // 5. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(order.getId());
        Mockito.verify(orderRepository, Mockito.times(1)).save(order);
        Mockito.verify(orderRepository, Mockito.never()).deleteById(Mockito.any(UUID.class));
        Mockito.verifyNoInteractions(orderMapper);
        Mockito.verifyNoInteractions(orderItemMapper);
    }

    /**
     * Tests updating an order when the order does not exist.
     *
     * <p>
     * Verifies that when an update is attempted for a non-existent order ID, the
     * service throws a {@link ProcessingException} and no update or save operations
     * are performed.
     * </p>
     */
    @Test
    void testForUpdateWhenOrderNotFoundShouldThrowProcessingException() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        orderDto.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ////////////////////////////////////////////////////////////////////////
        // 2. Mock behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderRepository.findById(orderDto.getId())).thenReturn(Optional.empty());

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method & verify exception
        ////////////////////////////////////////////////////////////////////////
        ProcessingException exception = Assertions.assertThrows(ProcessingException.class,
                () -> orderService.update(orderDto),
                "Expected update() to throw ProcessingException when order is not found");

        Assertions.assertEquals("Order not found with ID: " + orderDto.getId().toString(), exception.getMessage());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderDto.getId());
        Mockito.verify(orderMapper, Mockito.never()).update(Mockito.any(), Mockito.any(OrderDto.class));
        Mockito.verify(orderItemMapper, Mockito.never()).toEntity(Mockito.any(OrderItemDto.class));
        Mockito.verify(orderRepository, Mockito.never()).save(Mockito.any(Order.class));
    }

    /**
     * Tests patching an order when the order does not exist.
     *
     * <p>
     * Verifies that when a patch request is made for a non-existent order ID, the
     * service throws a {@link ProcessingException} and no update, mapping, or
     * persistence operations are performed.
     * </p>
     */
    @Test
    void testForPatchWhenOrderNotFoundShouldThrowProcessingException() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto patchDto = OrderTestHelper.createOrderDtoForPatch();

        ////////////////////////////////////////////////////////////////////////
        // 2. Mock behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderRepository.findById(patchDto.getId())).thenReturn(Optional.empty());

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method & verify exception
        ////////////////////////////////////////////////////////////////////////
        ProcessingException exception = Assertions.assertThrows(ProcessingException.class,
                () -> orderService.patch(patchDto),
                "Expected patch() to throw ProcessingException when order is not found");
        Assertions.assertEquals("Order not found with ID: " + patchDto.getId().toString(), exception.getMessage());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(patchDto.getId());
        Mockito.verify(orderMapper, Mockito.never()).patch(Mockito.any(Order.class), Mockito.any(OrderDto.class));
        Mockito.verify(orderItemMapper, Mockito.never()).toEntity(Mockito.any(OrderItemDto.class));
        Mockito.verify(orderRepository, Mockito.never()).save(Mockito.any(Order.class));
    }

    /**
     * Tests retrieving an order by ID when the order does not exist.
     *
     * <p>
     * Verifies that when a non-existent order ID is requested, the service throws a
     * {@link ResourceNotFoundException} and no mapping is performed.
     * </p>
     */
    @Test
    void testForGetWhenOrderNotFoundShouldThrowResourceNotFoundException() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ////////////////////////////////////////////////////////////////////////
        // 2. Mock behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method & verify exception
        ////////////////////////////////////////////////////////////////////////
        ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
                () -> orderService.get(orderId),
                "Expected get() to throw ResourceNotFoundException when order is not found");
        Assertions.assertEquals("Resource not found", exception.getMessage());
        Assertions.assertEquals("Order not found with ID: 00000000-0000-0000-0000-000000000001", exception.getDescription());

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderRepository, Mockito.times(1)).findById(orderId);
        Mockito.verify(orderMapper, Mockito.never()).toDto(Mockito.any(Order.class));
        Mockito.verifyNoInteractions(orderItemMapper);
    }

    /**
     * Tests successful creation of an order.
     *
     * <p>
     * Verifies that when a valid {@link OrderDto} is provided, the service
     * correctly maps the DTO to an entity, calculates the total amount, persists
     * the order, and returns the expected {@link OrderDto}.
     * </p>
     */
    @Test
    void testForCreateWithoutOrderItem() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDtoWithoutOrderItem();
        Order order = OrderTestHelper.createOrderEntity();
        order.getOrderItems().clear();

        ////////////////////////////////////////////////////////////////////////
        // 2. Mock mapper and repository behavior
        ////////////////////////////////////////////////////////////////////////
        Mockito.when(orderMapper.toEntity(orderDto)).thenReturn(order);
        Mockito.when(orderRepository.save(Mockito.any(Order.class))).thenReturn(order);
        Mockito.when(orderMapper.toDto(order)).thenReturn(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Call method under test
        ////////////////////////////////////////////////////////////////////////
        OrderDto actual = orderService.create(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify BUSINESS LOGIC
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertNotNull(actual, "Returned OrderDto must not be null");
        Assertions.assertEquals(BigDecimal.ZERO, order.getTotalAmount(),
                "Total amount must be ZERO when no order items are provided");
        Assertions.assertNotNull(order.getOrderItems(), "Order items list must not be null");
        Assertions.assertTrue(order.getOrderItems().isEmpty(),
                "Order items list must be empty when no items are provided");
        Assertions.assertEquals(orderDto.toString(), actual.toString(), "Returned DTO must represent the saved order");

        ////////////////////////////////////////////////////////////////////////
        // 5. Verify interactions (secondary, not primary)
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderMapper, Mockito.times(1)).toEntity(orderDto);
        Mockito.verify(orderRepository, Mockito.times(1)).save(order);
        Mockito.verify(orderMapper, Mockito.times(1)).toDto(order);
        Mockito.verifyNoInteractions(orderItemMapper);
    }
}
