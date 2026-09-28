package ai.agilery.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletContext;

/**
 * A Spring Boot {@link InfoContributor} implementation to provide additional
 * information about the application, including version and service name, by
 * reading the `META-INF/MANIFEST.MF` file.
 */
@Component
public class InfoEndPointApi implements InfoContributor {

    /**
     * ServletContext for accessing application level parameters and resources.
     */
    private final ServletContext servletContext;

    /**
     * Constructor for InfoEndPointApi.
     *
     * @param servletContext the ServletContext to be injected.
     */
    public InfoEndPointApi(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Contributes additional details using the specified builder.
     *
     * @param builder The builder to use.
     */
    @Override
    public void contribute(Info.Builder builder) {
        try {
            InputStream manifestStream = servletContext.getResourceAsStream("META-INF/MANIFEST.MF");

            if (manifestStream == null) {
                throw new IllegalStateException("MANIFEST.MF is missing in the war/jar");
            }

            Manifest manifest = new Manifest(manifestStream);
            Attributes attributes = manifest.getMainAttributes();
            String version = attributes.getValue("Implementation-Version");
            String serviceName = attributes.getValue("Implementation-Title");

            builder.withDetail("version", version);
            builder.withDetail("description", "The example micro-service.");
            builder.withDetail("serviceName", serviceName);

        } catch (IOException e) {
            throw new IllegalStateException("MANIFEST.MF is missing in the war/jar", e);
        }
    }
}
