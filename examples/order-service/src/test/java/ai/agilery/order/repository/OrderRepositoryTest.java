package ai.agilery.order.repository;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.TimeZone;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import ai.agilery.order.entity.Order;
import ai.agilery.order.entity.OrderStatus;
import ai.agilery.order.entity.PaymentMethod;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    // Set time zone of the service to UTC
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        Order o1 = new Order();
        o1.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o1.setModifiedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o1.setCustomerEmail("customerEmail1@example.com");
        o1.setCustomerName("customerName1");
        o1.setDeliveryAddress("deliveryAddress1");
        o1.setNotes("notes1");
        o1.setOrderStatus(OrderStatus.CONFIRMED);
        o1.setPaymentMethod(PaymentMethod.CASH);
        o1.setTotalAmount(BigDecimal.valueOf(0));

        orderRepository.save(o1);

        Order o2 = new Order();
        o2.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o2.setModifiedBy(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o2.setCustomerEmail("customerEmail2@example.com");
        o2.setCustomerName("customerName2");
        o2.setDeliveryAddress("deliveryAddress2");
        o2.setNotes("notes2");
        o2.setOrderStatus(OrderStatus.PENDING);
        o2.setPaymentMethod(PaymentMethod.UPI);
        o2.setTotalAmount(BigDecimal.valueOf(0));

        orderRepository.save(o2);

        orderRepository.flush();
    }

    @Test
    void testFindByCustomerEmail() {
        Assertions.assertTrue(orderRepository.findByCustomerEmail("customerEmail1@example.com").size() == 1);
        Assertions.assertTrue(orderRepository.findByCustomerEmail("customerEmail2@example.com").size() == 1);
        Assertions.assertTrue(orderRepository.findByCustomerEmail("customerEmail3@example.com").isEmpty());
    }

    @Test
    void testFindByIdAndCustomerEmail() {
        Order o1 = orderRepository.findByCustomerEmail("customerEmail1@example.com").get(0);
        Order o2 = orderRepository.findByCustomerEmail("customerEmail2@example.com").get(0);
        Assertions.assertTrue(
                orderRepository.findByIdAndCustomerEmail(o1.getId(), "customerEmail1@example.com").isPresent());
        Assertions.assertTrue(
                orderRepository.findByIdAndCustomerEmail(o2.getId(), "customerEmail2@example.com").isPresent());
        Assertions
        .assertTrue(orderRepository.findByIdAndCustomerEmail(UUID.fromString("00000000-0000-0000-0000-000000000001"), "customerEmail3@example.com")
                .isEmpty());
    }

    @Test
    void testFindByPaymentMethod() {
        Assertions.assertTrue(orderRepository.findByPaymentMethod(PaymentMethod.CASH).size() == 1);
        Assertions.assertTrue(orderRepository.findByPaymentMethod(PaymentMethod.UPI).size() == 1);
        Assertions.assertTrue(orderRepository.findByPaymentMethod(PaymentMethod.CREDIT_CARD).isEmpty());
        Assertions.assertTrue(orderRepository.findByPaymentMethod(PaymentMethod.DEBIT_CARD).isEmpty());

    }

    @Test
    void testFindByOrderStatus() {
        Assertions.assertTrue(orderRepository.findByOrderStatus(OrderStatus.PENDING).size() == 1);
        Assertions.assertTrue(orderRepository.findByOrderStatus(OrderStatus.CONFIRMED).size() == 1);
        Assertions.assertTrue(orderRepository.findByOrderStatus(OrderStatus.DELIVERED).isEmpty());
        Assertions.assertTrue(orderRepository.findByOrderStatus(OrderStatus.CANCELLED).isEmpty());
        Assertions.assertTrue(orderRepository.findByOrderStatus(OrderStatus.SHIPPED).isEmpty());
    }

    @Test
    void testFindByCreatedDateTimeBetween() {
        // Entry created between yesterday and today
        Assertions.assertTrue(orderRepository
                .findByCreatedDateTimeBetween(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))
                .size() == 2);

        // Entry created between yesterday and day before yesterday
        Assertions.assertTrue(orderRepository
                .findByCreatedDateTimeBetween(LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1))
                .isEmpty());

        // Entry created in future
        Assertions.assertTrue(orderRepository
                .findByCreatedDateTimeBetween(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2))
                .isEmpty());
    }

}
