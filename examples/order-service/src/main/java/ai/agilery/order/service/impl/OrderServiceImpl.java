package ai.agilery.order.service.impl;

import java.math.BigDecimal; // Ensure BigDecimal is imported
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.OrderItemDto;
import ai.agilery.order.entity.Order;
import ai.agilery.order.entity.OrderItem;
import ai.agilery.order.entity.OrderStatus; // Corrected import to use entity's OrderStatus
import ai.agilery.order.exceptions.ProcessingException;
import ai.agilery.order.exceptions.ResourceNotFoundException;
import ai.agilery.order.mapper.OrderItemMapper;
import ai.agilery.order.mapper.OrderMapper;
import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing customer orders.
 * Provides business logic for CRUD operations on orders, including their associated order items.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** Repository for performing CRUD operations on {@link Order} entities. */
    private final OrderRepository orderRepository;

    /** Mapper for converting between {@link OrderDto} and {@link Order} entities. */
    private final OrderMapper orderMapper;

    /** Mapper for converting between {@link OrderItemDto} and {@link OrderItem} entities. */
    private final OrderItemMapper orderItemMapper;

    /**
     * Creates a new order along with its associated order items.
     * The total amount of the order is calculated based on the sum of order item subtotals.
     *
     * @param orderDto The DTO containing the order details and a list of order items to be created.
     * @return The created OrderDto with generated ID and calculated fields.
     */
    @Override
    public OrderDto create(OrderDto orderDto) {
        log.info("Creating new order for customer: {}", orderDto.getCustomerName());

        Order order = orderMapper.toEntity(orderDto);

        // orderDate is automatically set by @CreationTimestamp on the entity, no need to set here.
        // orderStatus is mapped from DTO by orderMapper.toEntity(orderDto). If a specific initial status
        // is always required (e.g., PENDING), it should be enforced here or in validation.
        // Assuming DTO's orderStatus is the desired initial status, and it's validated to be valid for creation.

        // Calculate subtotal for each item and total amount for the order
        BigDecimal totalAmount = BigDecimal.ZERO; // Use BigDecimal for totalAmount
        if (orderDto.getOrderItems() != null && !orderDto.getOrderItems().isEmpty()) {
            for (OrderItemDto itemDto : orderDto.getOrderItems()) {
                // Perform BigDecimal arithmetic for subtotal
                BigDecimal itemSubtotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                totalAmount = totalAmount.add(itemSubtotal);
            }
            order.setTotalAmount(totalAmount);
        } else {
            order.setTotalAmount(BigDecimal.ZERO); // No items, total is 0
        }


        if (orderDto.getOrderItems() != null && !orderDto.getOrderItems().isEmpty()) {
            List<OrderItem> orderItems = orderDto.getOrderItems().stream()
                    .map(itemDto -> {
                        OrderItem item = orderItemMapper.toEntity(itemDto);
                        item.setOrder(order); // Link item to the saved order
                        return item;
                    })
                    .collect(Collectors.toList());
            order.setOrderItems(orderItems); // Update the saved order with its items
        }

        Order savedOrder = orderRepository.save(order);

        log.info("Order created with ID: {}", savedOrder.getId());

        return orderMapper.toDto(savedOrder);
    }

    /**
     * Updates the details of an existing order.
     * This operation updates only the order's main details, not its associated order items.
     *
     * @param orderDto The DTO containing the updated order details. Must include the order ID.
     * @return The updated OrderDto.
     * @throws ResourceNotFoundException if no order is found with the given ID.
     */
    @Override
    public OrderDto update(OrderDto orderDto) {
        log.info("Updating order with ID: {}", orderDto.getId());

        Order existingOrder = orderRepository.findById(orderDto.getId())
                .orElseThrow(() -> new ProcessingException("Order not found with ID: " + orderDto.getId()));

        orderMapper.update(existingOrder, orderDto); // Update main order fields, ignoring items and audit fields

        // Clear existing order items
        existingOrder.getOrderItems().clear();
        if (orderDto.getOrderItems() != null) {
            List<OrderItem> orderItems = orderDto.getOrderItems().stream()
                    .map(itemDto -> {
                        OrderItem item = orderItemMapper.toEntity(itemDto);
                        item.setOrder(existingOrder); // Link item to the saved order
                        return item;
                    })
                    .collect(Collectors.toList());
            // Add into the existing list of order items. Don't override the list with new list
            existingOrder.getOrderItems().addAll(orderItems); // Update the saved order with its items
        }

        Order updatedOrder = orderRepository.save(existingOrder);
        log.debug("Order with ID: {} updated successfully.", updatedOrder.getId());

        return orderMapper.toDto(updatedOrder);
    }

    /**
     * Partially updates an existing order, specifically its status and notes.
     * This method applies non-null values from the DTO to the existing order entity.
     *
     * @param orderDto The DTO containing the partial update details (e.g., new status, notes). Must include the order ID.
     * @return The patched OrderDto.
     * @throws ResourceNotFoundException if no order is found with the given ID.
     */
    @Override
    public OrderDto patch(OrderDto orderDto) {
        log.info("Patching order with ID: {}", orderDto.getId());

        Order existingOrder = orderRepository.findById(orderDto.getId())
                .orElseThrow(() -> new ProcessingException("Order not found with ID: " + orderDto.getId()));

        orderMapper.patch(existingOrder, orderDto); // Apply partial update for status and notes

        if (orderDto.getOrderItems() != null) {
            existingOrder.getOrderItems().clear();
            List<OrderItem> orderItems = orderDto.getOrderItems().stream()
                    .map(itemDto -> {
                        OrderItem item = orderItemMapper.toEntity(itemDto);
                        item.setOrder(existingOrder); // Link item to the saved order
                        return item;
                    })
                    .collect(Collectors.toList());
            // Add into the existing list of order items. Don't override the list with new list
            existingOrder.getOrderItems().addAll(orderItems); // Update the saved order with its items
        }

        Order patchedOrder = orderRepository.save(existingOrder);
        log.debug("Order with ID: {} patched successfully.", patchedOrder.getId());

        return orderMapper.toDto(patchedOrder);
    }

    /**
     * Retrieves a single order by its unique identifier.
     * The returned OrderDto will include all associated order items.
     *
     * @param id The unique ID of the order to retrieve.
     * @return The OrderDto corresponding to the given ID.
     * @throws ResourceNotFoundException if no order is found with the given ID.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDto get(UUID id) {
        log.info("Retrieving order with ID: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        return orderMapper.toDto(order);
    }

    /**
     * Retrieves a paginated and sortable list of all orders.
     *
     * @param pageable Pagination and sorting information.
     * @return A Page of OrderDto objects.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> getAll(Pageable pageable) {
        log.info("Retrieving all orders with pagination: {}", pageable);

        Page<Order> ordersPage = orderRepository.findAll(pageable);

        return ordersPage.map(orderMapper::toDto);
    }

    /**
     * Deletes an order by setting its status to CANCELLED (soft delete).
     * This operation does not physically remove the order from the database.
     *
     * @param id The unique ID of the order to delete (cancel).
     * @throws ResourceNotFoundException if no order is found with the given ID.
     */
    @Override
    public void delete(UUID id) {
        log.info("Attempting to soft delete (cancel) order with ID: {}", id);

        orderRepository.findById(id).ifPresent(existingOrder -> {
            existingOrder.setOrderStatus(OrderStatus.CANCELLED); // Uses entity's OrderStatus
            orderRepository.save(existingOrder);
            log.debug("Order with ID: {} successfully cancelled.", id);
        });

        // Hard delete example
        // orderRepository.deleteById(id);
    }
}
