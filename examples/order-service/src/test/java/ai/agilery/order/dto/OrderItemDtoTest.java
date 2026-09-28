package ai.agilery.order.dto;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class OrderItemDtoTest {

    /** Object1 for testing. */
    private OrderItemDto o1;

    /** Object 2 for testing */
    private OrderItemDto o2;

    /** Object 3 for testing */
    private OrderItemDto o3;

    /** Setup the required test data before starting the test case. */
    @BeforeEach
    void setUpTestData() {
        // Setup object for testing
        o1 = new OrderItemDto(UUID.fromString("00000000-0000-0000-0000-000000000001"), "productName", "productSku", 2, BigDecimal.valueOf(100.5),
                LocalDateTime.of(1970, 1, 1, 0, 0), LocalDateTime.of(1970, 1, 1, 0, 0), UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o2 = new OrderItemDto(UUID.fromString("00000000-0000-0000-0000-000000000001"), "productName", "productSku", 2, BigDecimal.valueOf(100.5),
                LocalDateTime.of(1970, 1, 1, 0, 0), LocalDateTime.of(1970, 1, 1, 0, 0), UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o3 = new OrderItemDto();
    }

    @Test
    void testGettersAndSetters() {
        // Verify Getters and Setters
        AssertionUtil.verifySetterGetters(o1, o2);
    }

    @Test
    void testHashCodeAndEquals() {
        // Verify Equals and Hashcode
        AssertionUtil.verifyHashAndEquals(o1, o2, o3);
    }

    @Test
    void testToString() {
        // Verify string representation of the object
        String expected = "OrderItemDto(id=00000000-0000-0000-0000-000000000001, productName=productName, productSku=productSku, quantity=2, "
                + "unitPrice=100.5, createdDateTime=1970-01-01T00:00, "
                + "lastModifiedDateTime=1970-01-01T00:00, createdBy=00000000-0000-0000-0000-000000000001, modifiedBy=00000000-0000-0000-0000-000000000001)";
        String actual = o1.toString();
        Assertions.assertEquals(expected, actual, "Expected and actual string representation should match");
    }

    @Test
    void testXmlPresentation() throws JsonProcessingException {
        // Verify XML representation
        String expectedXml = "<OrderItemDto><id>00000000-0000-0000-0000-000000000001</id><productName>productName</productName>"
                + "<productSku>productSku</productSku><quantity>2</quantity><unitPrice>100.5</unitPrice>"
                + "<createdDateTime>1970-01-01T00:00:00</createdDateTime>"
                + "<lastModifiedDateTime>1970-01-01T00:00:00</lastModifiedDateTime>"
                + "<createdBy>00000000-0000-0000-0000-000000000001</createdBy><modifiedBy>00000000-0000-0000-0000-000000000001</modifiedBy></OrderItemDto>";
        String actualXml = AssertionUtil.getXmlPresentation(o1);
        Assertions.assertEquals(expectedXml, actualXml,
                "Expected and actual XML representation of object should match");
    }

    @Test
    void testJsonPresentation() throws JsonProcessingException {
        // Verify JSON representation
        String expectedJson = "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"productName\":\"productName\",\"productSku\":\"productSku\","
                + "\"quantity\":2,\"unitPrice\":100.5,"
                + "\"createdDateTime\":\"1970-01-01T00:00:00\",\"lastModifiedDateTime\":\"1970-01-01T00:00:00\","
                + "\"createdBy\":\"00000000-0000-0000-0000-000000000001\",\"modifiedBy\":\"00000000-0000-0000-0000-000000000001\"}";
        String actualJson = AssertionUtil.getJsonPresentation(o1);
        Assertions.assertEquals(expectedJson, actualJson,
                "Expected and actual JSON representation of object should match");
    }
}
