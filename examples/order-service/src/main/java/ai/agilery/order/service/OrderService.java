package ai.agilery.order.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

import ai.agilery.order.dto.OrderDto;

/**
 * Service interface for managing customer orders.
 * Provides business logic for CRUD operations on orders, including their associated order items.
 */
public interface OrderService {

    /**
     * Creates a new order along with its associated order items.
     * The total amount of the order is calculated based on the sum of order item subtotals.
     *
     * @param orderDto The DTO containing the order details and a list of order items to be created.
     * @return The created OrderDto with generated ID and calculated fields.
     */
    OrderDto create(OrderDto orderDto);

    /**
     * Updates the details of an existing order.
     * This operation updates only the order's main details, not its associated order items.
     *
     * @param orderDto The DTO containing the updated order details. Must include the order ID.
     * @return The updated OrderDto.
     */
    OrderDto update(OrderDto orderDto);

    /**
     * Partially updates an existing order, specifically its status and notes.
     * This method applies non-null values from the DTO to the existing order entity.
     *
     * @param orderDto The DTO containing the partial update details (e.g., new status, notes). Must include the order ID.
     * @return The patched OrderDto.
     */
    OrderDto patch(OrderDto orderDto);

    /**
     * Retrieves a single order by its unique identifier.
     * The returned OrderDto will include all associated order items.
     *
     * @param id The unique ID of the order to retrieve.
     * @return The OrderDto corresponding to the given ID.
     * @throws ai.agilery.order.exceptions.ResourceNotFoundException if no order is found with the given ID.
     */
    OrderDto get(UUID id);

    /**
     * Retrieves a paginated and sortable list of all orders.
     *
     * @param pageable Pagination and sorting information.
     * @return A Page of OrderDto objects.
     */
    Page<OrderDto> getAll(Pageable pageable);

    /**
     * Deletes an order by setting its status to CANCELLED (soft delete).
     * This operation does not physically remove the order from the database.
     *
     * @param id The unique ID of the order to delete (cancel).
     * @throws ai.agilery.order.exceptions.ResourceNotFoundException if no order is found with the given ID.
     */
    void delete(UUID id);
}
