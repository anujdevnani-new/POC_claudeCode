package ai.agilery.order.mapper;

import java.util.UUID;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import ai.agilery.order.dto.OrderItemDto;
import ai.agilery.order.entity.OrderItem;
import ai.agilery.order.utils.OrderItemTestHelper;

class OrderItemMapperTest {

    private OrderItemMapper orderItemMapper = Mappers.getMapper(OrderItemMapper.class);

    /**
     * Tests the public API contract of {@link OrderItemMapper}.
     * <p>
     * Verifies that the mapper exposes only the expected methods and that no
     * additional or missing methods are introduced unintentionally.
     * </p>
     */
    @Test
    void testForOrderItemMapperShouldExposeOnlyExpectedMethods() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Expected public mapper methods (contract)
        ////////////////////////////////////////////////////////////////////////
        Set<String> expectedMethods = Set.of("toEntity", "toDto", "update", "patch");

        ////////////////////////////////////////////////////////////////////////
        // 2. Actual declared methods
        ////////////////////////////////////////////////////////////////////////
        Set<String> actualMethods = Arrays.stream(OrderItemMapper.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .map(Method::getName)
                .collect(Collectors.toSet());

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify contract
        ////////////////////////////////////////////////////////////////////////

        // Explicit count check (readability)
        Assertions.assertEquals(expectedMethods.size(), actualMethods.size(),
                "OrderItemMapper method count has changed");

        // Exact method-name match (behavior + structure)
        Assertions.assertEquals(expectedMethods, actualMethods,
                "OrderItemMapper has changed. If intentional, update tests.");
    }

    /**
     * Tests the conversion of {@link OrderItemDto} to {@link OrderItem}.
     * <p>
     * Verifies that business fields are mapped correctly and that audit and
     * read-only fields are ignored during the conversion.
     * </p>
     */
    @Test
    void testForToEntity() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderItemDto dto = OrderItemTestHelper.createOrderItemDto();

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        OrderItem entity = orderItemMapper.toEntity(dto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(dto.getProductName(), entity.getProductName());
        Assertions.assertEquals(dto.getProductSku(), entity.getProductSku());
        Assertions.assertEquals(dto.getQuantity(), entity.getQuantity());
        Assertions.assertEquals(dto.getUnitPrice(), entity.getUnitPrice());

        // Ignored / read-only fields
        Assertions.assertNull(entity.getId());
        Assertions.assertNull(entity.getCreatedDateTime());
        Assertions.assertNull(entity.getLastModifiedDateTime());
        Assertions.assertNull(entity.getCreatedBy());
        Assertions.assertNull(entity.getModifiedBy());
    }

    /**
     * Tests the conversion of {@link OrderItem} to {@link OrderItemDto}.
     * <p>
     * Verifies that business fields are correctly mapped from the entity to the DTO.
     * </p>
     */
    @Test
    void testForToDto() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderItemDto sourceDto = OrderItemTestHelper.createOrderItemDto();
        OrderItem entity = orderItemMapper.toEntity(sourceDto);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        OrderItemDto targetDto = orderItemMapper.toDto(entity);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(sourceDto.getProductName(), targetDto.getProductName());
        Assertions.assertEquals(sourceDto.getProductSku(), targetDto.getProductSku());
        Assertions.assertEquals(sourceDto.getQuantity(), targetDto.getQuantity());
        Assertions.assertEquals(sourceDto.getUnitPrice(), targetDto.getUnitPrice());
    }

    /**
     * Tests the update of an existing {@link OrderItem} using {@link OrderItemDto}.
     * <p>
     * Verifies that business fields are updated while identifier, audit fields,
     * and relationships remain unchanged.
     * </p>
     */
    @Test
    void testForUpdate() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderItem entity = OrderItemTestHelper.createOrderItemEntity();
        OrderItemDto dto = OrderItemTestHelper.createOrderItemDto();

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        orderItemMapper.update(entity, dto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////

        // Business fields updated
        Assertions.assertEquals(dto.getProductName(), entity.getProductName());
        Assertions.assertEquals(dto.getProductSku(), entity.getProductSku());
        Assertions.assertEquals(dto.getQuantity(), entity.getQuantity());
        Assertions.assertEquals(dto.getUnitPrice(), entity.getUnitPrice());

        // Protected fields unchanged
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), entity.getId());
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), entity.getCreatedBy());
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), entity.getModifiedBy());
        Assertions.assertNotNull(entity.getCreatedDateTime());
        Assertions.assertNotNull(entity.getLastModifiedDateTime());

        // Relationship untouched
        Assertions.assertNotNull(entity.getOrder());
    }

    /**
     * Tests the partial update of an existing {@link OrderItem} using {@link OrderItemDto}.
     * <p>
     * Verifies that only non-null fields are updated and that identifier, audit
     * fields, and relationships remain unchanged.
     * </p>
     */
    @Test
    void testForPatch() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderItem entity = OrderItemTestHelper.createOrderItemEntity();
        OrderItemDto patchDto = OrderItemTestHelper.createOrderItemDtoForPatch();

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        orderItemMapper.patch(entity, patchDto);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////

        // Non-null fields must be updated
        Assertions.assertEquals("PATCH-SKU-001", entity.getProductSku());
        Assertions.assertEquals(new BigDecimal("1500.00"), entity.getUnitPrice());

        // Null fields must NOT be overwritten
        Assertions.assertEquals("Old Product", entity.getProductName());
        Assertions.assertEquals(1, entity.getQuantity());

        // Protected fields unchanged
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), entity.getId());
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), entity.getCreatedBy());
        Assertions.assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), entity.getModifiedBy());
        Assertions.assertNotNull(entity.getCreatedDateTime());
        Assertions.assertNotNull(entity.getLastModifiedDateTime());

        // Relationship untouched
        Assertions.assertNotNull(entity.getOrder());
    }

}
