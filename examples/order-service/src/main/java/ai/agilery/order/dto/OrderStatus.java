package ai.agilery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration representing the possible statuses of an order.
 * This helps in tracking the lifecycle of an order from creation to delivery or cancellation.
 */
@Schema(description = "Represents the current status of an order.")
public enum OrderStatus {
    /**
     * The order has been placed but not yet processed or confirmed.
     */
    PENDING,
    /**
     * The order has been confirmed and is being prepared for shipment.
     */
    CONFIRMED,
    /**
     * The order has been shipped to the customer.
     */
    SHIPPED,
    /**
     * The order has been successfully delivered to the customer.
     */
    DELIVERED,
    /**
     * The order has been cancelled.
     */
    CANCELLED
}
