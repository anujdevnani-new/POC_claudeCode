package ai.agilery.order.entity;

/**
 * Enumeration representing the available payment methods for an order.
 */
public enum PaymentMethod {
    /**
     * Payment made via credit card.
     */
    CREDIT_CARD,
    /**
     * Payment made via debit card.
     */
    DEBIT_CARD,
    /**
     * Payment made via cash.
     */
    CASH,
    /**
     * Payment made via Unified Payments Interface (UPI).
     */
    UPI
}
