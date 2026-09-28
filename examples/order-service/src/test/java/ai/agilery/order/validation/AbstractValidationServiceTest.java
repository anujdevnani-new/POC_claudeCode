package ai.agilery.order.validation;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Contract tests for {@link AbstractValidationService}.
 *
 * <p>
 * Verifies that all default validation lifecycle methods are safe to invoke and
 * do not throw exceptions when using the default no-op implementations.
 */
class AbstractValidationServiceTest {

    /**
     * Dummy implementation used only to test default method behavior.
     */
    static class TestValidationService implements AbstractValidationService<String, UUID> {
        // No overrides – uses default methods
    }

    private final AbstractValidationService<String, UUID> validationService = new TestValidationService();

    /**
     * Verifies that the default {@code onCreate} method executes without throwing
     * any exceptions.
     */
    @Test
    void testForOnCreate() {
        Assertions.assertDoesNotThrow(() -> validationService.onCreate("test-dto"));
    }

    /**
     * Verifies that the default {@code onUpdate} method executes without throwing
     * any exceptions.
     */
    @Test
    void testForOnUpdate() {
        Assertions.assertDoesNotThrow(() -> validationService.onUpdate("test-dto"));
    }

    /**
     * Verifies that the default {@code onPatch} method executes without throwing
     * any exceptions.
     */
    @Test
    void testForOnPatch() {
        Assertions.assertDoesNotThrow(() -> validationService.onPatch("test-dto"));
    }

    /**
     * Verifies that the default {@code onGet} method executes without throwing any
     * exceptions.
     */
    @Test
    void testForOnGet() {
        Assertions.assertDoesNotThrow(() -> validationService.onGet(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }

    /**
     * Verifies that the default {@code onGetAll} method executes without throwing
     * any exceptions when provided with a valid {@link Pageable} instance.
     */
    @Test
    void testForOnGetAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Assertions.assertDoesNotThrow(() -> validationService.onGetAll(pageable));
    }
}
