package ai.agilery.order.dto;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class OrderDtoTest {

    /** Object1 for testing. */
    private OrderDto o1;

    /** Object 2 for testing */
    private OrderDto o2;

    /** Object 3 for testing */
    private OrderDto o3;

    /** Setup the required test data before starting the test case. */
    @BeforeEach
    void setUpTestData() {
        // Setup object for testing
        o1 = new OrderDto(UUID.fromString("00000000-0000-0000-0000-000000000001"), "customerName", "customerEmail", "deliveryAddress", "PENDING", BigDecimal.valueOf(201.5),
                "CREDIT_CARD",
                "notes", new ArrayList<OrderItemDto>(), LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(1970, 1, 1, 0, 0), UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o2 = new OrderDto(UUID.fromString("00000000-0000-0000-0000-000000000001"), "customerName", "customerEmail", "deliveryAddress", "PENDING", BigDecimal.valueOf(201.5),
                "CREDIT_CARD",
                "notes", new ArrayList<OrderItemDto>(), LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.of(1970, 1, 1, 0, 0), UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        o3 = new OrderDto();
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
        String expected = "OrderDto(id=00000000-0000-0000-0000-000000000001, customerName=customerName, customerEmail=customerEmail, "
                + "deliveryAddress=deliveryAddress, orderStatus=PENDING, totalAmount=201.5, "
                + "paymentMethod=CREDIT_CARD, notes=notes, orderItems=[], createdDateTime=1970-01-01T00:00, "
                + "lastModifiedDateTime=1970-01-01T00:00, createdBy=00000000-0000-0000-0000-000000000001, modifiedBy=00000000-0000-0000-0000-000000000001)";
        String actual = o1.toString();
        Assertions.assertEquals(expected, actual, "Expected and actual string representation should match");
    }

    @Test
    void testXmlPresentation() throws JsonProcessingException {
        // Verify XML representation
        String expectedXml = "<OrderDto><id>00000000-0000-0000-0000-000000000001</id><customerName>customerName</customerName>"
                + "<customerEmail>customerEmail</customerEmail><deliveryAddress>deliveryAddress</deliveryAddress>"
                + "<orderStatus>PENDING</orderStatus><totalAmount>201.5</totalAmount>"
                + "<paymentMethod>CREDIT_CARD</paymentMethod><notes>notes</notes><orderItems/>"
                + "<createdDateTime>1970-01-01T00:00:00</createdDateTime>"
                + "<lastModifiedDateTime>1970-01-01T00:00:00</lastModifiedDateTime>"
                + "<createdBy>00000000-0000-0000-0000-000000000001</createdBy><modifiedBy>00000000-0000-0000-0000-000000000001</modifiedBy></OrderDto>";
        String actualXml = AssertionUtil.getXmlPresentation(o1);
        Assertions.assertEquals(expectedXml, actualXml,
                "Expected and actual XML representation of object should match");
    }

    @Test
    void testJsonPresentation() throws JsonProcessingException {
        // Verify JSON representation
        String expectedJson = "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"customerName\":\"customerName\","
                + "\"customerEmail\":\"customerEmail\",\"deliveryAddress\":"
                + "\"deliveryAddress\",\"orderStatus\":\"PENDING\",\"totalAmount\":201.5,"
                + "\"paymentMethod\":\"CREDIT_CARD\",\"notes\":\"notes\",\"orderItems\":[],"
                + "\"createdDateTime\":\"1970-01-01T00:00:00\","
                + "\"lastModifiedDateTime\":\"1970-01-01T00:00:00\",\"createdBy\":\"00000000-0000-0000-0000-000000000001\",\"modifiedBy\":\"00000000-0000-0000-0000-000000000001\"}";
        String actualJson = AssertionUtil.getJsonPresentation(o1);
        Assertions.assertEquals(expectedJson, actualJson,
                "Expected and actual JSON representation of object should match");
    }

}
