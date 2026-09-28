package ai.agilery.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Configures global Spring Data Web support for pagination and sorting.
 * <p>
 * Enables stable JSON serialization for {@code Page<T>} responses by using
 * {@link PageSerializationMode#VIA_DTO}, preventing the "PageImpl" warning.
 * <br>
 * This ensures consistent and predictable paginated API responses across all
 * controllers.
 * </p>
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {
    // Empty
}
