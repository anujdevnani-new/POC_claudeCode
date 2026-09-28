package ai.agilery.order.config;

import org.springframework.http.HttpStatus;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

/**
 * Custom implementation of Feign's ErrorDecoder.
 */
public final class CustomErrorDecoder implements ErrorDecoder {

    /**
     * Retry interval in milliseconds.
     */
    private static final long RETRY_INTERVAL_IN_MS = 50L;

    /**
     * Decodes the HTTP response and returns an appropriate exception.
     *
     * @param method   the HTTP method being called
     * @param response the HTTP response from the Feign client
     * @return the exception to be thrown based on the response status
     */
    @Override
    public Exception decode(String method, Response response) {
        FeignException exception = FeignException.errorStatus(method, response);
        HttpStatus status = HttpStatus.resolve(response.status());

        if (status == HttpStatus.GATEWAY_TIMEOUT || status == HttpStatus.SERVICE_UNAVAILABLE) {
            return createRetryException(exception, response);
        } else {
            return exception;
        }
    }

    /**
     * Creates a retryable exception for retryable error statuses.
     *
     * @param exception the original FeignException
     * @param response  the HTTP response
     * @return a RetryableException for retryable statuses (503, 504)
     */
    private FeignException createRetryException(FeignException exception, Response response) {
        return new RetryableException(response.status(), exception.getMessage(), response.request().httpMethod(),
                exception, RETRY_INTERVAL_IN_MS, response.request());
    }
}
