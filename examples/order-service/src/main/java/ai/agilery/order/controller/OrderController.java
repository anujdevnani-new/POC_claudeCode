package ai.agilery.order.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.service.OrderService;
import ai.agilery.order.validation.OnCreate;
import ai.agilery.order.validation.OnPatch;
import ai.agilery.order.validation.OnUpdate;
import ai.agilery.order.validation.impl.service.OrderValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for managing orders.
 * Provides endpoints for creating, retrieving, updating, patching, and deleting orders.
 * All operations are delegated to {@link OrderService} after validation by {@link OrderValidationService}.
 */
@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Order Management", description = "APIs for managing customer orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    /**
     * Service for handling order-related business logic.
     */
    private final OrderService orderService;

    /**
     * Service for handling order-related validation logic.
     */
    private final OrderValidationService orderValidationService;

    /**
     * Creates a new order with its associated order items.
     *
     * @param dto The {@link OrderDto} containing the details of the order to be created.
     * @return A {@link ResponseEntity} containing the created {@link OrderDto} and HTTP status 201 (Created).
     */
    @PostMapping
    @Operation(summary = "Create a new order",
    description = "Creates a new order along with its order items. Performs syntactic and semantic validations.",
    responses = {
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<OrderDto> create(@Validated(OnCreate.class) @RequestBody OrderDto dto) {
        log.info("Received request to create order for customer: {}", dto.getCustomerName());
        orderValidationService.onCreate(dto);
        OrderDto createdOrder = orderService.create(dto);
        log.info("Order created successfully with ID: {}", createdOrder.getId());
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    /**
     * Updates an existing order's details (excluding order items).
     *
     * @param dto The {@link OrderDto} containing the updated details.
     * @param id  The ID of the order to be updated.
     * @return A {@link ResponseEntity} containing the updated {@link OrderDto} and HTTP status 200 (OK).
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing order",
    description = "Updates the details of an existing order identified by its ID. Order items cannot be updated via this endpoint.",
    responses = {
            @ApiResponse(responseCode = "200", description = "Order updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<OrderDto> update(@Validated(OnUpdate.class) @RequestBody OrderDto dto, @PathVariable UUID id) {
        log.info("Received request to update order with ID: {}", id);
        dto.setId(id);
        orderValidationService.onUpdate(dto);
        OrderDto updatedOrder = orderService.update(dto);
        log.info("Order with ID: {} updated successfully.", updatedOrder.getId());
        return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
    }

    /**
     * Partially updates an existing order, specifically its status and notes.
     *
     * @param dto The {@link OrderDto} containing the fields to be patched (status, notes).
     * @param id  The ID of the order to be patched.
     * @return A {@link ResponseEntity} containing the patched {@link OrderDto} and HTTP status 200 (OK).
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Partially update an order",
    description = "Applies a partial update to an order, typically for status transitions or notes. Only allows valid status transitions.",
    responses = {
            @ApiResponse(responseCode = "200", description = "Order patched successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or status transition",
            content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<OrderDto> patch(@Validated(OnPatch.class) @RequestBody OrderDto dto, @PathVariable UUID id) {
        log.info("Received request to patch order with ID: {}", id);
        dto.setId(id);
        orderValidationService.onPatch(dto);
        OrderDto patchedOrder = orderService.patch(dto);
        log.info("Order with ID: {} patched successfully.", patchedOrder.getId());
        return new ResponseEntity<>(patchedOrder, HttpStatus.OK);
    }

    /**
     * Retrieves an order by its unique ID, including its associated order items.
     *
     * @param id The ID of the order to retrieve.
     * @return A {@link ResponseEntity} containing the {@link OrderDto} if found, and HTTP status 200 (OK).
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID",
    description = "Retrieves a single order by its unique identifier, including all associated order items.",
    responses = {
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<OrderDto> get(@PathVariable UUID id) {
        log.info("Received request to get order with ID: {}", id);
        orderValidationService.onGet(id);
        OrderDto order = orderService.get(id);
        log.info("Order with ID: {} retrieved successfully.", id);
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    /**
     * Retrieves a paginated list of all orders.
     *
     * @param pageable Pagination and sorting information.
     * @return A {@link ResponseEntity} containing a {@link Page} of {@link OrderDto} and HTTP status 200 (OK).
     */
    @GetMapping
    @Operation(summary = "Get all orders with pagination",
    description = "Retrieves a paginated and sortable list of all orders.",
    responses = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Page<OrderDto>> getAll(@ParameterObject Pageable pageable) {
        log.info("Received request to get all orders with pageable: {}", pageable);
        orderValidationService.onGetAll(pageable);
        Page<OrderDto> orders = orderService.getAll(pageable);
        log.info("Retrieved {} orders for page: {}", orders.getNumberOfElements(), pageable.getPageNumber());
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    /**
     * Deletes an order by its ID. This is a soft delete operation, setting the order status to CANCELLED.
     *
     * @param id The ID of the order to be deleted (cancelled).
     * @return A {@link ResponseEntity} with no content and HTTP status 204 (No Content).
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel an order (soft delete)",
    description = "Performs a soft delete on an order by setting its status to CANCELLED. Only orders in PENDING or CONFIRMED status can be cancelled.",
    responses = {
            @ApiResponse(responseCode = "204", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order status for cancellation",
            content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Received request to delete (cancel) order with ID: {}", id);
        orderValidationService.onDelete(id);
        orderService.delete(id);
        log.info("Order with ID: {} cancelled successfully.", id);
        return ResponseEntity.noContent().build();
    }
}
