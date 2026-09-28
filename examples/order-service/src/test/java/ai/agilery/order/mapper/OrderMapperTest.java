package ai.agilery.order.mapper;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.entity.Order;
import ai.agilery.order.utils.OrderTestHelper;

/**
 * Unit tests for {@link OrderMapper}.
 * <p>
 * Validates MapStruct-based mappings between {@link OrderDto} and
 * {@link Order}, including full conversion, update, and partial update (patch)
 * scenarios.
 * </p>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OrderMapperTest.TestConfig.class)
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * Spring test configuration that loads only mapper components.
     */
    @ComponentScan(basePackages = "ai.agilery.order.mapper")
    static class TestConfig {
        // No beans needed explicitly
    }


    /**
     * Tests the DTO to entity mapping behavior of {@link OrderMapper}.
     *
     * <p>
     * Verifies correct field mapping, enum conversion, nested item mapping, and
     * exclusion of audit fields.
     * </p>
     */
    @Test
    void testForToEntity() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto dto = OrderTestHelper.createOrderDto();

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        Order entity = orderMapper.toEntity(dto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertNotNull(entity);

        // Basic fields
        Assertions.assertEquals(dto.getTotalAmount(), entity.getTotalAmount());
        Assertions.assertEquals(dto.getCustomerName(), entity.getCustomerName());
        Assertions.assertEquals(dto.getCustomerEmail(), entity.getCustomerEmail());
        Assertions.assertEquals(dto.getDeliveryAddress(), entity.getDeliveryAddress());
        Assertions.assertEquals(dto.getNotes(), entity.getNotes());

        Assertions.assertEquals(ai.agilery.order.entity.OrderStatus.PENDING, entity.getOrderStatus());
        Assertions.assertEquals(ai.agilery.order.entity.PaymentMethod.CREDIT_CARD, entity.getPaymentMethod());

        Assertions.assertNotNull(entity.getOrderItems());
        Assertions.assertEquals(1, entity.getOrderItems().size());

        Assertions.assertNull(entity.getCreatedBy());
        Assertions.assertNull(entity.getCreatedDateTime());
        Assertions.assertNull(entity.getLastModifiedDateTime());
    }

    /**
     * Tests entity to DTO mapping of {@link OrderMapper}.
     *
     * <p>
     * Verifies correct field mapping, enum conversion, and nested order item
     * mapping.
     * </p>
     */
    @Test
    void testForToDto() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Order entity = OrderTestHelper.createOrderEntity();

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        OrderDto dto = orderMapper.toDto(entity);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertNotNull(dto);

        // Basic fields
        Assertions.assertEquals(entity.getTotalAmount(), dto.getTotalAmount());
        Assertions.assertEquals(entity.getCustomerName(), dto.getCustomerName());
        Assertions.assertEquals(entity.getCustomerEmail(), dto.getCustomerEmail());
        Assertions.assertEquals(entity.getDeliveryAddress(), dto.getDeliveryAddress());
        Assertions.assertEquals(entity.getNotes(), dto.getNotes());
        Assertions.assertEquals(ai.agilery.order.dto.OrderStatus.PENDING.toString(), dto.getOrderStatus());
        Assertions.assertEquals(ai.agilery.order.dto.PaymentMethod.CREDIT_CARD.toString(), dto.getPaymentMethod());

        // Nested mapping
        Assertions.assertNotNull(dto.getOrderItems());
        Assertions.assertEquals(1, dto.getOrderItems().size());
    }

    /**
     * Tests update mapping of {@link OrderMapper}.
     *
     * <p>
     * Verifies that updatable fields are modified while restricted fields (ID and
     * audit data) remain unchanged.
     * </p>
     */
    @Test
    void testForUpdate() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Order entity = OrderTestHelper.createOrderEntity();
        OrderDto dto = OrderTestHelper.createOrderDto();

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        orderMapper.update(entity, dto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////

        // Basic fields
        Assertions.assertEquals(dto.getTotalAmount(), entity.getTotalAmount());
        Assertions.assertEquals(entity.getCustomerName(), dto.getCustomerName());
        Assertions.assertEquals(entity.getCustomerEmail(), dto.getCustomerEmail());
        Assertions.assertEquals(entity.getDeliveryAddress(), dto.getDeliveryAddress());
        Assertions.assertEquals(entity.getNotes(), dto.getNotes());
        Assertions.assertEquals(ai.agilery.order.entity.OrderStatus.PENDING, entity.getOrderStatus());
        Assertions.assertEquals(ai.agilery.order.entity.PaymentMethod.CREDIT_CARD, entity.getPaymentMethod());
        Assertions.assertNotNull(dto.getOrderItems());
        Assertions.assertEquals(1, dto.getOrderItems().size());

        // Must NOT change
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), entity.getId());
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), entity.getCreatedBy());
    }

    /**
     * Tests partial update (patch) mapping of {@link OrderMapper}.
     *
     * <p>
     * Verifies that only non-null fields from the DTO are applied and existing
     * values are preserved for null fields.
     * </p>
     */
    @Test
    void testForPatch() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Order entity = OrderTestHelper.createOrderEntity();
        OrderDto patchDto = OrderTestHelper.createOrderDtoForPatch();

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        orderMapper.patch(entity, patchDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(patchDto.getTotalAmount(), entity.getTotalAmount());

        // Unchanged fields
        Assertions.assertEquals(ai.agilery.order.entity.OrderStatus.PENDING, entity.getOrderStatus());
        Assertions.assertEquals(ai.agilery.order.entity.PaymentMethod.CREDIT_CARD, entity.getPaymentMethod());
    }

}
