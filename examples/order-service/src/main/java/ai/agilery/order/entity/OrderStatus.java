package ai.agilery.order.entity;

/**
 * Enumeration representing the possible statuses of an order.
 */
public enum OrderStatus {
    /**
     * The order has been placed but not yet processed.
     */
    PENDING,
    /**
     * The order has been confirmed and is ready for fulfillment.
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
