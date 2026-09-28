package ai.agilery.order.dto;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class ErrorResponseDtoTest {

    /** Object1 for testing. */
    private ErrorResponseDto o1;

    /** Object 2 for testing */
    private ErrorResponseDto o2;

    /** Object 3 for testing */
    private ErrorResponseDto o3;

    /** Setup the required test data before starting the test case. */
    @BeforeEach
    void setUpTestData() {
        // Setup object for testing
        o1 = new ErrorResponseDto("message", "description", new ArrayList<ValidationErrorDto>());
        o2 = new ErrorResponseDto("message", "description", new ArrayList<ValidationErrorDto>());
        o3 = new ErrorResponseDto();
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
        String expected = "ErrorResponseDto(message=message, description=description, validationErrors=[])";
        String actual = o1.toString();
        Assertions.assertEquals(expected, actual, "Expected and actual string representation should match");
    }


    @Test
    void testXmlPresentation() throws JsonProcessingException {
        // Verify XML representation
        String expectedXml = "<ErrorResponseDto><message>message</message><description>description</description><validationErrors/></ErrorResponseDto>";
        String actualXml = AssertionUtil.getXmlPresentation(o1);
        Assertions.assertEquals(expectedXml, actualXml,
                "Expected and actual XML representation of object should match");
    }

    @Test
    void testJsonPresentation() throws JsonProcessingException {
        // Verify JSON representation
        String expectedJson = "{\"message\":\"message\",\"description\":\"description\",\"validationErrors\":[]}";
        String actualJson = AssertionUtil.getJsonPresentation(o1);
        Assertions.assertEquals(expectedJson, actualJson,
                "Expected and actual JSON representation of object should match");
    }

}
