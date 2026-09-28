package ai.agilery.order.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.agilery.order.entity.Order;
import ai.agilery.order.entity.OrderStatus;
import ai.agilery.order.entity.PaymentMethod;

/**
 * Repository interface for the {@link Order} entity.
 * <p>
 * Provides standard CRUD operations and custom query methods for managing customer orders.
 * </p>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Finds all orders associated with a specific customer email.
     *
     * @param customerEmail The email address of the customer.
     * @return A list of {@link Order} entities matching the customer email.
     */
    List<Order> findByCustomerEmail(String customerEmail);

    /**
     * Finds all orders with a specific order status.
     *
     * @param orderStatus The status of the order (e.g., PENDING, SHIPPED, DELIVERED).
     * @return A list of {@link Order} entities matching the specified status.
     */
    List<Order> findByOrderStatus(OrderStatus orderStatus);

    /**
     * Finds all orders placed within a specific date range.
     *
     * @param startDate The start date and time of the range (inclusive).
     * @param endDate The end date and time of the range (inclusive).
     * @return A list of {@link Order} entities placed within the specified date range.
     */
    List<Order> findByCreatedDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds an order by its unique identifier and customer email.
     *
     * @param id The unique identifier of the order.
     * @param customerEmail The email address of the customer.
     * @return An {@link Optional} containing the {@link Order} if found, or empty otherwise.
     */
    Optional<Order> findByIdAndCustomerEmail(UUID id, String customerEmail);

    /**
     * Finds all orders with a specific payment method.
     *
     * @param paymentMethod The payment method used for the order.
     * @return A list of {@link Order} entities matching the specified payment method.
     */
    List<Order> findByPaymentMethod(PaymentMethod paymentMethod);
}
