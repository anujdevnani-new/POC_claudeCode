package ai.agilery.example;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * The main entry point for the Account Service Application.
 * <p>
 * This class initializes and configures the Spring Boot application. It
 * includes configuration for component scanning, Feign clients, and Hystrix for
 * fault tolerance.
 * </p>
 * <ul>
 * <li>@SpringBootApplication: Marks this as a Spring Boot application.</li>
 * <li>@ComponentScan: Specifies the base packages to scan for Spring
 * components.</li>
 * <li>@EnableFeignClients: Enables Feign clients for declarative REST client
 * implementation.</li>
 * <li>@EnableHystrix: Enables Hystrix for circuit breaker and fault tolerance
 * functionality.</li>
 * </ul>
 */
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableFeignClients
@EnableHystrix
public class ExampleServiceApplication extends SpringBootServletInitializer {

    // Set time zone of the service to UTC
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args) {
        SpringApplication.run(ExampleServiceApplication.class, args);
    }

    /**
     * Configures the application when deployed in a servlet container.
     *
     * @param builder the SpringApplicationBuilder used to configure the application
     * @return the configured SpringApplicationBuilder
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return super.configure(builder);
    }
}
