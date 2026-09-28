package ai.agilery.order.config;

import java.util.UUID;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;

/**
 * Unit tests for {@link CustomErrorDecoder}.
 *
 * <p>
 * These tests verify that retryable HTTP statuses (503, 504) are converted into
 * {@link RetryableException}, while all other statuses result in a
 * {@link FeignException}.
 *
 * <p>
 * This is a pure unit test:
 * <ul>
 * <li>No Spring context</li>
 * <li>No mocking frameworks</li>
 * <li>Direct Feign objects</li>
 * </ul>
 */
class CustomErrorDecoderTest {

    private final CustomErrorDecoder decoder = new CustomErrorDecoder();

    /**
     * Verifies that HTTP 503 (Service Unavailable) results in a RetryableException.
     */
    @Test
    void testForRetryableExceptionWhenServiceUnavailable() {

        Response response = createResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), Request.HttpMethod.GET);

        Exception exception = decoder.decode("GET", response);

        Assertions.assertTrue(exception instanceof RetryableException);

        RetryableException retryable = (RetryableException) exception;
        Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), retryable.status());
        Assertions.assertEquals(50L, retryable.retryAfter());
        Assertions.assertEquals(Request.HttpMethod.GET, retryable.method());
    }

    /**
     * Verifies that HTTP 504 (Gateway Timeout) results in a RetryableException.
     */
    @Test
    void testForRetryableExceptionWhenGatewayTimeout() {

        Response response = createResponse(HttpStatus.GATEWAY_TIMEOUT.value(), Request.HttpMethod.POST);

        Exception exception = decoder.decode("POST", response);

        Assertions.assertTrue(exception instanceof RetryableException);
        RetryableException retryable = (RetryableException) exception;
        Assertions.assertEquals(HttpStatus.GATEWAY_TIMEOUT.value(), retryable.status());
        Assertions.assertEquals(50L, retryable.retryAfter());
        Assertions.assertEquals(Request.HttpMethod.POST, retryable.method());
    }

    /**
     * Verifies that non-retryable HTTP statuses return a FeignException.
     */
    @Test
    void testForFeignExceptionWhenStatusIsNonRetryable() {

        Response response = createResponse(HttpStatus.BAD_REQUEST.value(), Request.HttpMethod.GET);

        Exception exception = decoder.decode("GET", response);
        Assertions.assertTrue(exception instanceof FeignException);
        Assertions.assertFalse(exception instanceof RetryableException);
    }

    /**
     * Helper method to create a Feign {@link Response} with a specific HTTP status
     * and HTTP method.
     */
    private Response createResponse(int status, Request.HttpMethod method) {

        Request request = Request.create(method, "/test", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return Response.builder().status(status).reason("reason").request(request).headers(Collections.emptyMap())
                .build();
    }
}
