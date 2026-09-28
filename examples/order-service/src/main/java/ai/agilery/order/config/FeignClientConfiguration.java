package ai.agilery.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Retryer;
import feign.codec.ErrorDecoder;

/**
 * Configures the Feign client settings, including retry and error decoding
 * behavior.
 */
@Configuration
public class FeignClientConfiguration {

    /**
     * Maximum wait for feign client in milliseconds.
     */
    @Value("${feign.client.retry.maximum-period}")
    private Long maximumPeriod;

    /**
     * Maximum number of retry attempts.
     */
    @Value("${feign.client.retry.maximum-attempts}")
    private Integer maxAttempts;

    /**
     * Backoff period in milliseconds.
     */
    @Value("${feign.client.retry.backoff-period}")
    private Integer backoffPeriod;

    /**
     * Provides the custom error decoder bean for handling Feign client errors.
     *
     * @return The custom error decoder
     */
    @Bean
    ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    /**
     * Configures the Feign client's retry policy.
     *
     * @return The configured retryer with custom timeout and max attempts.
     */
    @Bean
    Retryer retryer() {
        return new Retryer.Default(backoffPeriod, maximumPeriod, maxAttempts);
    }
}
