package ai.agilery.order.dto;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class AssertionUtil<T> {

    /**
     * The constant OBJECT_MAPPER.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * The constant XML_MAPPER.
     */
    private static final XmlMapper XML_MAPPER = (XmlMapper) new XmlMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AssertionUtil() {
        // Private constructor
    }

    /**
     * Asserts that two objects are equal by comparing their JSON representations.
     *
     * @param actual   The actual object
     * @param expected The expected object
     * @param <T>      The type of the objects being compared
     */
    public static <T> void verifySetterGetters(T actual, T expected) {
        try {
            if (actual instanceof OffsetDateTime && expected instanceof OffsetDateTime) {
                Assertions.assertEquals(((OffsetDateTime) expected).toInstant(), ((OffsetDateTime) actual).toInstant(),
                        "The OffsetDateTime objects do not match. Getters and Setters might be incorrect.");
            } else {
                String actualJson = OBJECT_MAPPER.writeValueAsString(actual);
                String expectedJson = OBJECT_MAPPER.writeValueAsString(expected);
                Assertions.assertEquals(expectedJson, actualJson,
                        "The JSON representations of the objects do not match. Getters and Setters might be incorrect.");
            }
        } catch (Exception e) {
            Assertions.fail(String.format("Failed to compare JSON representations: %s, %s", actual, expected));
        }
    }

    /**
     * Verifies the equals and hashCode methods for the given objects.
     *
     * @param o1  The first object
     * @param o2  The second object (should be equal to o1)
     * @param o3  The third object (should not be equal to o1)
     * @param <T> The type of the objects being compared
     *
     */
    public static <T> void verifyHashAndEquals(T o1, T o2, T o3) {
        // Verify against null
        Assertions.assertNotNull(o1, "o1 should have not been null.");
        Assertions.assertNotNull(o2, "o2 should have not been null.");
        Assertions.assertNotNull(o3, "o3 should have not been null.");

        boolean isEqual = o1.equals(null);
        Assertions.assertFalse(isEqual, "o1 should have not been equal to null.");
        isEqual = o2.equals(null);
        Assertions.assertFalse(isEqual, "o2 should have not been equal to null.");
        isEqual = o3.equals(null);
        Assertions.assertFalse(isEqual, "o3 should have not been equal to null.");

        // Verify same reference
        Assertions.assertEquals(o1, o1, "o1 should have been equal to itself.");
        Assertions.assertEquals(o2, o2, "o2 should have been equal to itself.");
        Assertions.assertEquals(o3, o3, "o3 should have been equal to itself.");

        // Verify against different type of object
        isEqual = o1.equals("Other Type Object");
        Assertions.assertFalse(isEqual, "o1 should have not been equal to other types.");
        isEqual = o2.equals("Other Type Object");
        Assertions.assertFalse(isEqual, "o2 should have not been equal to other types.");
        isEqual = o3.equals("Other Type Object");
        Assertions.assertFalse(isEqual, "o3 should have not been equal to other types.");

        // Verify equality between o1 and o2
        Assertions.assertEquals(o1, o2, "o1 and o2 should have been equal.");
        Assertions.assertEquals(o1.hashCode(), o2.hashCode(), "Hash codes of o1 and o2 should have been equal.");

        // Verify inequality between o1 and o3
        Assertions.assertNotEquals(o1, o3, "o1 and o3 objects should have not been equal.");
        Assertions.assertNotEquals(o2, o3, "o2 and o3 objects should have not been equal.");
        Assertions.assertNotEquals(o1.hashCode(), o3.hashCode(),
                "Hash codes of o1 and o3 should have not been equal. ");
        Assertions.assertNotEquals(o2.hashCode(), o3.hashCode(),
                "Hash codes of o2 and o3 should have not been equal. ");

    }

    /**
     * This method returns XML representation of the object.
     *
     * @param o   The object
     * @param <T> The type of the objects being compared
     * @return The XML string.
     *
     * @throws JsonProcessingException In case of any processing error.
     */
    public static <T> String getXmlPresentation(T o) throws JsonProcessingException {
        return XML_MAPPER.writeValueAsString(o);
    }

    /**
     * This method returns JSON representation of the object.
     *
     * @param o   The object.
     * @param <T> The type of the objects being compared
     * @return The JSON string.
     *
     * @throws JsonProcessingException In case of any processing error.
     */
    public static <T> String getJsonPresentation(T o) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(o);
    }
}
