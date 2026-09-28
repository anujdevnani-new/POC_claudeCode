package ai.agilery.order.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * Unit tests for {@link OpenApiConfig}.
 *
 * <p>
 * Verifies that OpenAPI metadata declared via annotations matches the expected
 * contract exactly.
 */
class OpenApiConfigTest {

    /**
     * Verifies that {@link OpenAPIDefinition} is present and contains the exact
     * expected OpenAPI metadata values.
     */
    @Test
    void testForExactOpenApiMetadata() {

        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);

        Assertions.assertNotNull(definition, "OpenAPIDefinition must be present");

        Info info = definition.info();
        Assertions.assertNotNull(info);

        // ----- Title & Version placeholders -----
        Assertions.assertEquals("${spring.application.name}", info.title());
        Assertions.assertEquals("${spring.application.version}", info.version());

        // ----- Contact details -----
        Contact contact = info.contact();
        Assertions.assertNotNull(contact);

        Assertions.assertEquals("Agilery", contact.name());
        Assertions.assertEquals("contacts@agilery.ai", contact.email());
        Assertions.assertEquals("https://www.agilery.ai", contact.url());

        // ----- Description (exact match) -----
        String expectedDescription = "Order Service is responsible for managing the entire lifecycle of customer orders within the system.\n"
                + "It provides REST APIs to create, view, update, and manage orders and their associated order items.\n"
                + "The service ensures data consistency through transactional operations and enforces business rules such as\n"
                + "order status validation, item-level subtotal/total calculation, and audit tracking." + "" + "\n"
                + "\n" + "## Key Capabilities" + "\n" + "- Create new orders with one or more order items\n" + "\n"
                + "- Retrieve order details, including nested items and calculated totals\n" + "\n"
                + "- Update existing orders and order items with proper validation\n" + "\n"
                + "- Support partial updates (PATCH) while maintaining bidirectional relations\n" + "\n"
                + "- Automatically handle orphan removal for deleted items\n" + "\n"
                + "- Manage order statuses (e.g., PENDING, CONFIRMED, SHIPPED, CANCELLED)\n" + "\n"
                + "- Audit fields for tracking creation and modification metadata";

        Assertions.assertEquals(expectedDescription, info.description(), "OpenAPI description must match exactly");
    }
}
