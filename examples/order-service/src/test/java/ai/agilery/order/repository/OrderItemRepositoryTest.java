package ai.agilery.order.repository;

import java.util.UUID;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import ai.agilery.order.entity.Order;
import ai.agilery.order.entity.OrderItem;
import ai.agilery.order.entity.OrderStatus;
import ai.agilery.order.entity.PaymentMethod;

/**
 * Repository test class for {@link OrderItemRepository}.
 * <p>
 * Verifies the correctness of custom query methods defined in
 * {@link OrderItemRepository} using an in-memory database.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
class OrderItemRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Order order1;
    private Order order2;

    /**
     * Sets up test data before each test execution.
     * <p>
     * Creates and persists {@link Order} and {@link OrderItem} entities to ensure a
     * consistent database state for repository testing.
     * </p>
     */
    @BeforeEach
    void setUp() {

        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        Order o1 = new Order();
        o1.setCustomerName("Customer One");
        o1.setCustomerEmail("customer1@test.com");
        o1.setDeliveryAddress("Address 1");
        o1.setOrderStatus(OrderStatus.PENDING);
        o1.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        o1.setTotalAmount(BigDecimal.valueOf(300));

        order1 = orderRepository.save(o1);

        Order o2 = new Order();
        o2.setCustomerName("Customer Two");
        o2.setCustomerEmail("customer2@test.com");
        o2.setDeliveryAddress("Address 2");
        o2.setOrderStatus(OrderStatus.CONFIRMED);
        o2.setPaymentMethod(PaymentMethod.UPI);
        o2.setTotalAmount(BigDecimal.valueOf(150));

        order2 = orderRepository.save(o2);

        OrderItem item1 = new OrderItem();
        item1.setOrder(order1);
        item1.setProductName("Product A");
        item1.setProductSku("SKU-A");
        item1.setQuantity(2);
        item1.setUnitPrice(BigDecimal.valueOf(100));
        orderItemRepository.save(item1);

        OrderItem item2 = new OrderItem();
        item2.setOrder(order1);
        item2.setProductName("Product B");
        item2.setProductSku("SKU-B");
        item2.setQuantity(1);
        item2.setUnitPrice(BigDecimal.valueOf(100));
        orderItemRepository.save(item2);

        OrderItem item3 = new OrderItem();
        item3.setOrder(order2);
        item3.setProductName("Product C");
        item3.setProductSku("SKU-A");
        item3.setQuantity(3);
        item3.setUnitPrice(BigDecimal.valueOf(50));
        orderItemRepository.save(item3);

        orderItemRepository.flush();
    }

    /**
     * Tests retrieval of order items by {@link Order}.
     * <p>
     * Verifies that only items associated with the given order are returned, and
     * that non-existing orders return no results.
     * </p>
     */
    @Test
    void testFindByOrder() {

        Assertions.assertEquals(2, orderItemRepository.findByOrder(order1).size());
        Assertions.assertEquals(1, orderItemRepository.findByOrder(order2).size());

        Order nonExistingOrder = new Order();
        nonExistingOrder.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        Assertions.assertTrue(orderItemRepository.findByOrder(nonExistingOrder).isEmpty());
    }

    /**
     * Tests retrieval of an order item by order and product name.
     * <p>
     * Verifies that matching order items are found correctly and non-existing
     * product names return empty results.
     * </p>
     */
    @Test
    void testFindByOrderAndProductName() {

        Assertions.assertTrue(orderItemRepository.findByOrderAndProductName(order1, "Product A").isPresent());
        Assertions.assertTrue(orderItemRepository.findByOrderAndProductName(order1, "Product B").isPresent());
        Assertions.assertTrue(orderItemRepository.findByOrderAndProductName(order2, "Product C").isPresent());
        Assertions.assertTrue(orderItemRepository.findByOrderAndProductName(order1, "Non Existing Product").isEmpty());
    }

    /**
     * Tests retrieval of order items by product SKU.
     * <p>
     * Verifies that all order items matching the given SKU are returned, and that
     * unknown SKUs produce empty results.
     * </p>
     */
    @Test
    void testFindByProductSku() {

        Assertions.assertEquals(2, orderItemRepository.findByProductSku("SKU-A").size());
        Assertions.assertEquals(1, orderItemRepository.findByProductSku("SKU-B").size());
        Assertions.assertTrue(orderItemRepository.findByProductSku("SKU-UNKNOWN").isEmpty());
    }
}
