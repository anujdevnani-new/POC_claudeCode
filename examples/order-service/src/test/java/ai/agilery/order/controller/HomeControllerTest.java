package ai.agilery.order.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ai.agilery.order.utils.TestHelper;

/**
 * Test class for {@link HomeController}.
 *
 * <p>
 * Verifies redirection behavior for root endpoint ("/").
 * </p>
 */
@WebMvcTest(controllers = HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Tests redirection from root URL ("/") to Swagger UI.
     * <p>
     * Ensures that the application responds with HTTP status {@code 302 (FOUND)}
     * and sets the {@code Location} header to {@code swagger-ui.html}.
     * </p>
     */
    @Test
    void testRedirectToSwaggerUi() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.get("/").contentType(MediaType.APPLICATION_JSON)).andReturn()
                .getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 2. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify HTTP status
        Assertions.assertEquals(HttpStatus.FOUND.value(), response.getStatus(), "HTTP status should be 302 (FOUND)");

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify response headers
        ////////////////////////////////////////////////////////////////////////
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(headerName -> headerName, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForSwaggerEndpointTesting();

        Assertions.assertEquals(expectedHeaders, actualHeaders,
                "Expected and actual headers do not match for redirect response");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify response body
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertTrue(response.getContentAsString().isEmpty(), "Response body should be empty for redirect");
    }
}
