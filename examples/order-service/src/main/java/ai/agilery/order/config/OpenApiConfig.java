package ai.agilery.order.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * Configuration class for OpenAPI (Swagger) documentation setup.
 *
 * <p>
 * This class configures the Swagger UI and OpenAPI metadata, and registers the
 * Bearer token authentication scheme so that secured APIs are properly
 * displayed in Swagger.
 * </p>
 *
 * <p>
 * Note: This only configures the API documentation. Actual authentication and
 * authorization logic is handled by Spring Security.
 * </p>
 */
@Configuration
@OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(
          title = "${spring.application.name}",
          version = "${spring.application.version}",
          contact = @Contact(
            name = "Agilery", email = "contacts@agilery.ai", url = "https://www.agilery.ai"
          ),

          description = "Order Service is responsible for managing the entire lifecycle of customer orders within the system.\n"
                  + "It provides REST APIs to create, view, update, and manage orders and their associated order items.\n"
                  + "The service ensures data consistency through transactional operations and enforces business rules such as\n"
                  + "order status validation, item-level subtotal/total calculation, and audit tracking."
                  + ""
                  + "\n"
                  + "\n"
                  + "## Key Capabilities"
                  + "\n"
                  + "- Create new orders with one or more order items\n"
                  + "\n"
                  + "- Retrieve order details, including nested items and calculated totals\n"
                  + "\n"
                  + "- Update existing orders and order items with proper validation\n"
                  + "\n"
                  + "- Support partial updates (PATCH) while maintaining bidirectional relations\n"
                  + "\n"
                  + "- Automatically handle orphan removal for deleted items\n"
                  + "\n"
                  + "- Manage order statuses (e.g., PENDING, CONFIRMED, SHIPPED, CANCELLED)\n"
                  + "\n"
                  + "- Audit fields for tracking creation and modification metadata"
        ),
        security = {
                @SecurityRequirement(name = "BearerAuth")
        }
      )
@SecuritySchemes({
    @SecurityScheme(
            name = "BearerAuth",
            type = SecuritySchemeType.HTTP,
            scheme = "bearer",
            bearerFormat = "JWT"
            )
})
public class OpenApiConfig {

}
