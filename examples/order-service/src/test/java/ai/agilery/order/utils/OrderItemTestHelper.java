package ai.agilery.order.utils;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ai.agilery.order.dto.OrderItemDto;
import ai.agilery.order.entity.Order;
import ai.agilery.order.entity.OrderItem;

/**
 * Utility class for creating reusable {@link OrderItem} and {@link OrderItemDto}
 * objects for test cases.
 */
public class OrderItemTestHelper {

    /**
     * Private constructor to prevent instantiation. This class is intended to be
     * used only via its static helper methods.
     */
    private OrderItemTestHelper() {}

    /**
     * Creates a valid {@link OrderItemDto} for test cases.
     * <p>
     * Returns a fully populated DTO suitable for create and update related tests.
     * </p>
     * @return a valid {@link OrderItemDto}
     */
    public static OrderItemDto createOrderItemDto() {

        OrderItemDto dto = new OrderItemDto();
        dto.setProductName("Laptop Pro X");
        dto.setProductSku("LPX-2023-001");
        dto.setQuantity(2);
        dto.setUnitPrice(new BigDecimal("1200.00"));

        return dto;
    }

    /**
     * Creates an existing {@link OrderItem} entity for test cases.
     * <p>
     * Simulates an entity loaded from the database with audit fields populated.
     * </p>
     * @return an existing {@link OrderItem} entity
     */
    public static OrderItem createOrderItemEntity() {

        OrderItem entity = new OrderItem();
        entity.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        entity.setProductName("Old Product");
        entity.setProductSku("OLD-SKU");
        entity.setQuantity(1);
        entity.setUnitPrice(new BigDecimal("100.00"));

        entity.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        entity.setModifiedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        entity.setCreatedDateTime(LocalDateTime.now().minusDays(1));
        entity.setLastModifiedDateTime(LocalDateTime.now().minusHours(5));

        Order order = new Order();
        entity.setOrder(order);
        return entity;
    }

    /**
     * Creates an {@link OrderItemDto} for PATCH operation test cases.
     * <p>
     * Populates only fields expected to be updated during a partial update.
     * </p>
     * @return an {@link OrderItemDto} for patch tests
     */
    public static OrderItemDto createOrderItemDtoForPatch() {

        OrderItemDto dto = new OrderItemDto();

        // Intentionally partial update
        dto.setProductSku("PATCH-SKU-001");
        dto.setUnitPrice(new BigDecimal("1500.00"));
        // productName & quantity left NULL on purpose
        return dto;
    }
}
