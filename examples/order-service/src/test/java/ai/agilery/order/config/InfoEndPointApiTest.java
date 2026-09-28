package ai.agilery.order.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.info.Info;

import jakarta.servlet.ServletContext;

/**
 * Unit tests for {@link InfoEndPointApi}.
 *
 * <p>
 * These tests verify that application metadata is correctly read from
 * MANIFEST.MF and exposed via the Spring Boot info endpoint.
 *
 * <p>
 * This is a pure unit test: no Spring context is started.
 */
class InfoEndPointApiTest {

    /**
     * Verifies that version and service name are correctly read from MANIFEST.MF
     * and added to the Info builder.
     */
    @Test
    void testForManifestDetailsWhenManifestIsPresent() {
        // Arrange
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        String manifestContent = """
                Manifest-Version: 1.0
                Implementation-Version: 1.2.3
                Implementation-Title: order-service
                """;

        ByteArrayInputStream manifestStream = new ByteArrayInputStream(
                manifestContent.getBytes(StandardCharsets.UTF_8));

        Mockito.when(servletContext.getResourceAsStream("META-INF/MANIFEST.MF")).thenReturn(manifestStream);

        InfoEndPointApi infoEndPointApi = new InfoEndPointApi(servletContext);
        Info.Builder builder = new Info.Builder();

        infoEndPointApi.contribute(builder);
        Info info = builder.build();
        Map<String, Object> details = info.getDetails();

        Assertions.assertEquals("1.2.3", details.get("version"));
        Assertions.assertEquals("order-service", details.get("serviceName"));
        Assertions.assertEquals("The example micro-service.", details.get("description"));
    }

    /**
     * Verifies that an exception is thrown when MANIFEST.MF is missing from the
     * application.
     */
    @Test
    void testForIllegalStateExceptionWhenManifestIsMissing() {
        // Arrange
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getResourceAsStream("META-INF/MANIFEST.MF")).thenReturn(null);

        InfoEndPointApi infoEndPointApi = new InfoEndPointApi(servletContext);
        Info.Builder builder = new Info.Builder();

        // Act & Assert
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> infoEndPointApi.contribute(builder));

        Assertions.assertTrue(exception.getMessage().contains("MANIFEST.MF is missing"),
                "Expected exception message to mention missing MANIFEST.MF");
    }

    /**
     * Verifies that an IOException while reading MANIFEST.MF is wrapped into an
     * IllegalStateException.
     */
    @Test
    void testForIllegalStateExceptionWhenManifestReadFails() {

        // Arrange
        ServletContext servletContext = Mockito.mock(ServletContext.class);

        InputStream faultyStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated I/O failure");
            }
        };

        Mockito.when(servletContext.getResourceAsStream("META-INF/MANIFEST.MF")).thenReturn(faultyStream);

        InfoEndPointApi infoEndPointApi = new InfoEndPointApi(servletContext);
        Info.Builder builder = new Info.Builder();

        // Act & Assert
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> infoEndPointApi.contribute(builder));

        Assertions.assertTrue(exception.getMessage().contains("MANIFEST.MF is missing in the war/jar"),
                "Expected IllegalStateException with MANIFEST.MF error message");
        Assertions.assertNotNull(exception.getCause(), "IOException should be wrapped as the cause");
        Assertions.assertTrue(exception.getCause() instanceof IOException, "Cause must be an IOException");
    }

}
