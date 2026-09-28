package ai.agilery.order.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

/**
 * Utility class providing predefined HTTP header maps commonly used across
 * controller unit tests and integration tests.
 * <p>
 * This class centralizes construction of frequently used header sets to ensure
 * consistency across the test suite and to reduce duplication.
 * </p>
 *
 * <p>
 * All methods return new {@code Map<String, List<String>>} instances on every
 * invocation to avoid unintended shared state across tests.
 * </p>
 */
public final class TestHelper {

    /**
     * Private constructor to prevent instantiation. This class is intended to be
     * used only via its static helper methods.
     */
    private TestHelper() {
        // private constructor.
    }

    /**
     * Returns a standard set of HTTP headers typically used when testing controller
     * endpoints in unit tests.
     * <p>
     * The returned headers include:
     * <ul>
     * <li>{@code Content-Type}: {@code application/json}</li>
     * <li>{@code X-Client-ID}: {@code Unknown}</li>
     * </ul>
     *
     * @return a new map containing headers for controller testing
     */
    public static Map<String, List<String>> getHeadersForTestingOfControllers() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Lists.newArrayList("application/json"));
        headers.put("X-Client-ID", Lists.newArrayList("Unknown"));
        return headers;
    }

    /**
     * Returns a standard set of HTTP headers commonly used for positive-path
     * integration testing.
     * <p>
     * The returned headers use lowercase header names to reflect typical wire-level
     * HTTP behavior and include:
     * <ul>
     * <li>{@code content-type}: {@code application/json}</li>
     * <li>{@code transfer-encoding}: {@code chunked}</li>
     * <li>{@code x-client-id}: {@code Unknown}</li>
     * </ul>
     *
     * @return a new map containing headers for integration testing
     */
    public static Map<String, List<String>> getHeadersForIntegrationTesting() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", Lists.newArrayList("application/json"));
        headers.put("transfer-encoding", Lists.newArrayList("chunked"));
        headers.put("x-client-id", Lists.newArrayList("Unknown"));
        return headers;
    }

    /**
     * Returns a set of HTTP headers used for negative or edge-case integration
     * testing scenarios.
     * <p>
     * These headers represent cases such as closed connection behavior, and
     * include:
     * <ul>
     * <li>{@code content-type}: {@code application/json}</li>
     * <li>{@code transfer-encoding}: {@code chunked}</li>
     * <li>{@code connection}: {@code close}</li>
     * <li>{@code x-client-id}: {@code Unknown}</li>
     * </ul>
     *
     * @return a new map containing headers for negative integration tests
     */
    public static Map<String, List<String>> getHeadersForIntegrationTestingNegative() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("content-type", Lists.newArrayList("application/json"));
        headers.put("transfer-encoding", Lists.newArrayList("chunked"));
        headers.put("connection", Lists.newArrayList("close"));
        headers.put("x-client-id", Lists.newArrayList("Unknown"));
        return headers;
    }

    /**
     * Returns headers used for testing DELETE method endpoints in controller unit
     * tests.
     * <p>
     * DELETE requests in these scenarios do not require a {@code Content-Type}
     * header, and only the following header is included:
     * <ul>
     * <li>{@code X-Client-ID}: {@code Unknown}</li>
     * </ul>
     *
     * @return a new map containing headers for controller DELETE method testing
     */
    public static Map<String, List<String>> getHeadersForTestingDeleteMethodOfControllers() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Client-ID", Lists.newArrayList("Unknown"));
        return headers;
    }

    /**
     * Returns headers used when performing integration tests for DELETE operations.
     * <p>
     * Similar to controller-level DELETE tests, these headers include:
     * <ul>
     * <li>{@code x-client-id}: {@code Unknown}</li>
     * </ul>
     *
     * @return a new map containing headers for integration DELETE testing
     */
    public static Map<String, List<String>> getHeadersForDeleteMethodOfIntegrationTesting() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("x-client-id", Lists.newArrayList("Unknown"));
        return headers;
    }

    /**
     * Returns HTTP headers for testing the Swagger UI redirection endpoint.
     * <p>
     * These headers are expected when accessing the root URL ("/"), which
     * redirects to {@code swagger-ui.html}.
     *
     * @return a new map containing headers for Swagger endpoint testing
     */
    public static Map<String, List<String>> getHeadersForSwaggerEndpointTesting() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Client-ID", Lists.newArrayList("Unknown"));
        headers.put("Content-Language", Lists.newArrayList("en"));
        headers.put("Location", Lists.newArrayList("swagger-ui.html"));
        return headers;
    }

}
