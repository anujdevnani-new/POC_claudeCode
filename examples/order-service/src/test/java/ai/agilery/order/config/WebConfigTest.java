package ai.agilery.order.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Unit tests for {@link WebConfig}.
 *
 * <p>
 * This test verifies the presence and correctness of configuration annotations.
 * It does not start a Spring context and acts as a safeguard against accidental
 * removal or modification of critical annotations.
 */
class WebConfigTest {

    /**
     * Verifies that {@link WebConfig} is annotated with {@link Configuration}.
     */
    @Test
    void testForConfigurationAnnotationPresence() {
        Assertions.assertTrue(WebConfig.class.isAnnotationPresent(Configuration.class),
                "WebConfig must be annotated with @Configuration");
    }

    /**
     * Verifies that {@link WebConfig} enables Spring Data Web support.
     */
    @Test
    void testForEnableSpringDataWebSupportAnnotationPresence() {
        Assertions.assertTrue(WebConfig.class.isAnnotationPresent(EnableSpringDataWebSupport.class),
                "WebConfig must be annotated with @EnableSpringDataWebSupport");
    }

    /**
     * Verifies that Page serialization mode is set to VIA_DTO.
     */
    @Test
    void testForPageSerializationModeViaDto() {
        EnableSpringDataWebSupport annotation = WebConfig.class.getAnnotation(EnableSpringDataWebSupport.class);

        Assertions.assertNotNull(annotation, "@EnableSpringDataWebSupport must be present");
        Assertions.assertEquals(PageSerializationMode.VIA_DTO, annotation.pageSerializationMode(),
                "PageSerializationMode must be VIA_DTO");
    }
}
