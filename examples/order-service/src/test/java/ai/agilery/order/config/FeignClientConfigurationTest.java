package ai.agilery.order.config;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import feign.Retryer;
import feign.codec.ErrorDecoder;

/**
 * Pure unit tests for {@link FeignClientConfiguration}.
 *
 * <p>
 * These tests do not start the Spring context and verify only the Java-level
 * behavior of the configuration class.
 * </p>
 */
class FeignClientConfigurationTest {

    /**
     * Verifies that the {@link ErrorDecoder} bean method returns an instance of
     * {@link CustomErrorDecoder}.
     */
    @Test
    void testForErrorDecoderBeanCreation() {
        FeignClientConfiguration config = new FeignClientConfiguration();

        ErrorDecoder errorDecoder = config.errorDecoder();

        Assertions.assertNotNull(errorDecoder);
        Assertions.assertTrue(errorDecoder instanceof CustomErrorDecoder);
    }

    /**
     * Verifies that the {@link Retryer} bean method returns a
     * {@link Retryer.Default} instance when configuration values are provided
     * manually.
     */
    @Test
    void testForRetryerBeanCreation() throws Exception {
        FeignClientConfiguration config = new FeignClientConfiguration();

        setField(config, "maximumPeriod", 5000L);
        setField(config, "maxAttempts", 3);
        setField(config, "backoffPeriod", 1000);

        Retryer retryer = config.retryer();

        Assertions.assertNotNull(retryer);
        Assertions.assertTrue(retryer instanceof Retryer.Default);
    }

    /**
     * Utility method to inject private fields using reflection. Required
     * because @Value is not processed in unit tests.
     */
    private static void setField(Object target, String fieldName, Object value) throws Exception {

        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
