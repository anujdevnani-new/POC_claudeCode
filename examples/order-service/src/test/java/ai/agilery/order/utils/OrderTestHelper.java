package ai.agilery.order.utils;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.OrderItemDto;
import ai.agilery.order.entity.Order;
import ai.agilery.order.entity.OrderItem;
import ai.agilery.order.entity.OrderStatus;
import ai.agilery.order.entity.PaymentMethod;

/**
 * Utility class for creating reusable {@link OrderDto} test data.
 */
public class OrderTestHelper {

    /**
     * Private constructor to prevent instantiation. This class is intended to be
     * used only via its static helper methods.
     */
    private OrderTestHelper() {}

    /**
     * Creates a valid {@link OrderDto} for use in test cases.
     * <p>
     * Returns a fully populated order DTO with one order item, suitable for
     * create and update related tests.
     * </p>
     * @return a valid {@link OrderDto}
     */
    public static OrderDto createOrderDto() {

        OrderItemDto orderItem = new OrderItemDto();
        orderItem.setProductName("Laptop Pro X");
        orderItem.setProductSku("LPX-2023-001");
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("1200.00"));

        OrderDto orderDto = new OrderDto();

        orderDto.setTotalAmount(new BigDecimal("2400.00"));
        orderDto.setCustomerName("John Doe");
        orderDto.setCustomerEmail("john.doe@example.com");
        orderDto.setDeliveryAddress("123 Main Street, Bangalore, Karnataka, 560001");
        orderDto.setOrderStatus("PENDING");
        orderDto.setPaymentMethod("CREDIT_CARD");
        orderDto.setNotes("Please deliver after 6 PM");
        orderDto.setOrderItems(List.of(orderItem));

        return orderDto;
    }

    /**
     * Creates an {@link OrderDto} for PATCH operation test cases.
     * <p>
     * Populates only fields expected to be updated during a partial update.
     * </p>
     * @return an {@link OrderDto} for patch tests
     */
    public static OrderDto createOrderDtoForPatch() {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        orderDto.setTotalAmount(new BigDecimal("2400.00"));
        return orderDto;
    }

    /**
     * Creates a fully populated {@link Order} entity for use in unit tests.
     * <p>
     * Simulates an existing persisted order by setting business fields, audit
     * metadata, and associated order items. This method is primarily used for
     * update and patch mapper test scenarios.
     * </p>
     * @return a populated {@link Order} entity suitable for mapper tests
     */
    public static Order createOrderEntity() {

        OrderItem orderItem = new OrderItem();
        orderItem.setProductName("Laptop Pro X");
        orderItem.setProductSku("LPX-2023-001");
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("1200.00"));

        Order order = new Order();
        order.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        order.setCustomerName("John Doe");
        order.setCustomerEmail("john.doe@example.com");
        order.setDeliveryAddress("123 Main Street, Bangalore, Karnataka, 560001");
        order.setOrderStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("2400.00"));
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setNotes("Please deliver after 6 PM");

        order.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        order.setModifiedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        order.setCreatedDateTime(LocalDateTime.now().minusDays(1));
        order.setLastModifiedDateTime(LocalDateTime.now());

        orderItem.setOrder(order);
        order.getOrderItems().add(orderItem);
        return order;
    }

    /**
     * Creates a valid {@link OrderDto} without any order items.
     *
     * <p>
     * Returns an order DTO populated with all mandatory business fields but
     * intentionally excludes {@code orderItems}. This helper is primarily used
     * to test service-layer behavior when creating orders without items, ensuring
     * that item-related processing and mappings are safely skipped.
     * </p>
     *
     * @return a valid {@link OrderDto} without order items
     */
    public static OrderDto createOrderDtoWithoutOrderItem() {

        OrderDto orderDto = new OrderDto();

        orderDto.setTotalAmount(new BigDecimal("2400.00"));
        orderDto.setCustomerName("John Doe");
        orderDto.setCustomerEmail("john.doe@example.com");
        orderDto.setDeliveryAddress("123 Main Street, Bangalore, Karnataka, 560001");
        orderDto.setOrderStatus("PENDING");
        orderDto.setPaymentMethod("CREDIT_CARD");
        orderDto.setNotes("Please deliver after 6 PM");
        return orderDto;
    }
}
