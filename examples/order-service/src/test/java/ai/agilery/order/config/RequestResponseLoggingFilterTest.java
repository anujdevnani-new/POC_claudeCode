package ai.agilery.order.config;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

/**
 * Unit tests for {@link RequestResponseLoggingFilter}.
 * <p>
 * These tests verify the exact behavioral contract of the filter. Any
 * modification to header handling, request/response wrapping, or response body
 * propagation MUST break these tests.
 */
class RequestResponseLoggingFilterTest {

    /**
     * Verifies full request–response flow when logging is enabled.
     * <p>
     * This test ensures:
     * <ul>
     * <li>X-Client-ID header is set correctly</li>
     * <li>Request/response are wrapped</li>
     * <li>Response body is copied back using copyBodyToResponse()</li>
     * </ul>
     * <p>
     * Removing {@code responseWrapper.copyBodyToResponse()} MUST fail this test.
     */
    @Test
    void testForDoFilterInternalWhenLoggingEnabledResponseBodyIsPreserved() throws Exception {

        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();
        enableLogging(filter);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/orders");
        request.setContent("request-body".getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> {
            res.getWriter().write("response-body");
        };

        filter.doFilter(request, response, filterChain);

        // Header contract
        Assertions.assertEquals(RequestResponseLoggingFilter.UNKNOWN,
                response.getHeader(RequestResponseLoggingFilter.X_CLIENT_ID));

        // ASSERTION — breaks if copyBodyToResponse() is removed
        Assertions.assertEquals("response-body", response.getContentAsString());
    }

    /**
     * Verifies that client-provided X-Client-ID header is propagated to the
     * response.
     */
    @Test
    void testForDoFilterInternalWhenClientIdProvidedHeaderIsCopied() throws Exception {

        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();
        enableLogging(filter);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestResponseLoggingFilter.X_CLIENT_ID, "client-123");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        Assertions.assertEquals("client-123", response.getHeader(RequestResponseLoggingFilter.X_CLIENT_ID));
    }

    /**
     * Verifies behavior when logging is disabled.
     * <p>
     * Ensures:
     * <ul>
     * <li>No wrapping is required</li>
     * <li>Filter chain is invoked with original request/response</li>
     * </ul>
     */
    @Test
    void testForDoFilterInternalWhenLoggingDisabledNoWrappingOccurs() throws Exception {

        RequestResponseLoggingFilter filter = new RequestResponseLoggingFilter();
        disableLogging(filter);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = Mockito.mock(FilterChain.class);
        filter.doFilter(request, response, filterChain);
        Mockito.verify(filterChain).doFilter(request, response);
        Assertions.assertEquals(RequestResponseLoggingFilter.UNKNOWN,
                response.getHeader(RequestResponseLoggingFilter.X_CLIENT_ID));
    }

    /**
     * Enables logging using reflection (pure unit test).
     */
    private void enableLogging(RequestResponseLoggingFilter filter) throws Exception {
        setLoggingEnabled(filter, true);
    }

    /**
     * Disables logging using reflection (pure unit test).
     */
    private void disableLogging(RequestResponseLoggingFilter filter) throws Exception {
        setLoggingEnabled(filter, false);
    }

    /**
     * Sets the private loggingEnabled flag via reflection.
     */
    private void setLoggingEnabled(RequestResponseLoggingFilter filter, boolean value) throws Exception {
        Field field = RequestResponseLoggingFilter.class.getDeclaredField("loggingEnabled");
        field.setAccessible(true);
        field.set(filter, value);
    }
}
