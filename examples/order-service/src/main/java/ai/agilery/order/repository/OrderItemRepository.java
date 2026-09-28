package ai.agilery.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.agilery.order.entity.Order;
import ai.agilery.order.entity.OrderItem;

/**
 * Repository interface for the {@link OrderItem} entity.
 * <p>
 * Provides standard CRUD operations and custom query methods for managing individual items within an order.
 * </p>
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Finds all order items belonging to a specific order.
     *
     * @param order The {@link Order} entity to which the items belong.
     * @return A list of {@link OrderItem} entities associated with the given order.
     */
    List<OrderItem> findByOrder(Order order);

    /**
     * Finds an order item by its associated order and product name.
     *
     * @param order The {@link Order} entity to which the item belongs.
     * @param productName The name of the product in the order item.
     * @return An {@link Optional} containing the {@link OrderItem} if found, or empty otherwise.
     */
    Optional<OrderItem> findByOrderAndProductName(Order order, String productName);

    /**
     * Finds all order items with a specific product SKU.
     *
     * @param productSku The SKU (Stock Keeping Unit) of the product.
     * @return A list of {@link OrderItem} entities matching the specified product SKU.
     */
    List<OrderItem> findByProductSku(String productSku);
}
