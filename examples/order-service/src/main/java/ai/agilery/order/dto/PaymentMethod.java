package ai.agilery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration representing the available payment methods for an order.
 */
@Schema(description = "Represents the payment method used for an order.")
public enum PaymentMethod {
    /**
     * Payment made using a credit card.
     */
    CREDIT_CARD,
    /**
     * Payment made using a debit card.
     */
    DEBIT_CARD,
    /**
     * Payment made using cash on delivery or in person.
     */
    CASH,
    /**
     * Payment made using Unified Payments Interface (UPI).
     */
    UPI
}
