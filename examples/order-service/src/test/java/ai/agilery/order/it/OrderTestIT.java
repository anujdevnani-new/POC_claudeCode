package ai.agilery.order.it;

import java.util.UUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.agilery.order.config.IntegrationTest;
import ai.agilery.order.dto.ErrorResponseDto;
import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.repository.OrderRepository;
import ai.agilery.order.utils.IntegrationTestDataHelper;
import ai.agilery.order.utils.OrderTestHelper;
import ai.agilery.order.utils.TestHelper;

/**
 * Integration test suite for Order REST APIs.
 *
 * <p>
 * This class validates the end-to-end behavior of the Order module by executing
 * real HTTP calls against the running application context.
 * </p>
 *
 * <p>
 * The tests cover:
 * <ul>
 * <li>Positive scenarios for all Order APIs</li>
 * <li>Syntax validation failures (request format / binding errors)</li>
 * <li>Semantic validation failures (business rule violations)</li>
 * <li>HTTP status codes, response headers, and response bodies</li>
 * </ul>
 * </p>
 *
 * <p>
 * These tests use {@link TestRestTemplate} and a real database setup to ensure
 * correctness of controller, validation chain, service, repository, and
 * exception handling layers together.
 * </p>
 */
@IntegrationTest
public class OrderTestIT extends IntegrationTestDataHelper {

    private final ObjectMapper objectMapper;
    private final TestRestTemplate testRestTemplate;

    @Autowired
    public OrderTestIT(ObjectMapper objectMapper, OrderRepository orderRepository, TestRestTemplate testRestTemplate) {
        super(orderRepository);
        this.objectMapper = objectMapper;
        this.testRestTemplate = testRestTemplate;
    }

    /**
     * Integration test for the CREATE Order API.
     *
     * <p>
     * Verifies the following scenarios:
     * <ul>
     * <li>Successful order creation with valid input (201 Created)</li>
     * <li>Syntax validation failure when required fields are missing (400 Bad
     * Request)</li>
     * <li>Semantic validation failure when business rules are violated (400 Bad
     * Request)</li>
     * </ul>
     * </p>
     *
     * <p>
     * The test validates HTTP status codes, response headers, and response bodies
     * for all scenarios.
     * </p>
     */
    @Test
    void testCreate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // COMMON HEADERS SETUP
        ////////////////////////////////////////////////////////////////////////
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 1: POSITIVE CASE
        // Valid request → Order created successfully → 201 CREATED
        ////////////////////////////////////////////////////////////////////////

        // 1. Test data setup
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        ObjectNode requestJson = objectMapper.valueToTree(orderDto);

        HttpEntity<String> request = new HttpEntity<>(requestJson.toString(), headers);

        // 2. Call CREATE API
        ResponseEntity<String> response = testRestTemplate.postForEntity("/api/orders", request, String.class);

        // 3. Verify HTTP status
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // 4. Verify response body is present
        Assertions.assertNotNull(response.getBody());

        // 5. Parse ACTUAL response JSON
        ObjectNode actualJson = (ObjectNode) objectMapper.readTree(response.getBody());

        // 6. Build EXPECTED JSON from request DTO
        ObjectNode expectedJson = objectMapper.valueToTree(orderDto);

        // 7. Copy GENERATED root-level fields
        expectedJson.put("id", actualJson.get("id").asText());
        expectedJson.set("createdDateTime", actualJson.get("createdDateTime"));
        expectedJson.set("lastModifiedDateTime", actualJson.get("lastModifiedDateTime"));

        // 8. Normalize & copy GENERATED fields inside orderItems
        for (int i = 0; i < actualJson.withArray("orderItems").size(); i++) {

            ObjectNode actualItem = (ObjectNode) actualJson.withArray("orderItems").get(i);

            ObjectNode expectedItem = (ObjectNode) expectedJson.withArray("orderItems").get(i);

            expectedItem.put("id", actualItem.get("id").asText());
            expectedItem.set("createdDateTime", actualItem.get("createdDateTime"));
            expectedItem.set("lastModifiedDateTime", actualItem.get("lastModifiedDateTime"));
        }

        // 9. Assert JSON equality
        JSONAssert.assertEquals(expectedJson.toString(), actualJson.toString(), true);

        // 10. Verify response headers
        Map<String, String> expectedHeaders = TestHelper.getHeadersForIntegrationTesting().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> actualHeaders = response.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> response.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(actualHeaders.get("date"));
        actualHeaders.remove("date");

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 2: SYNTAX VALIDATION FAILURE
        // Missing customerName → 400 BAD REQUEST
        ////////////////////////////////////////////////////////////////////////

        // 1. Prepare invalid JSON (customerName missing)
        ObjectNode syntaxInvalidJson = objectMapper.createObjectNode();
        syntaxInvalidJson.put("customerEmail", orderDto.getCustomerEmail());
        syntaxInvalidJson.put("deliveryAddress", orderDto.getDeliveryAddress());
        syntaxInvalidJson.put("orderStatus", orderDto.getOrderStatus());
        syntaxInvalidJson.put("paymentMethod", orderDto.getPaymentMethod());
        syntaxInvalidJson.set("orderItems", objectMapper.valueToTree(orderDto.getOrderItems()));

        HttpEntity<String> syntaxInvalidRequest = new HttpEntity<>(syntaxInvalidJson.toString(), headers);

        // 2. Call CREATE API
        ResponseEntity<String> syntaxFailureResponse = testRestTemplate.postForEntity("/api/orders",
                syntaxInvalidRequest, String.class);

        // 3. Verify HTTP status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, syntaxFailureResponse.getStatusCode());

        // 4. Build EXPECTED syntax validation error response
        ErrorResponseDto expectedSyntaxError = new ErrorResponseDto();
        expectedSyntaxError.setMessage("Request is not valid");
        expectedSyntaxError.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto syntaxValidationError = new ValidationErrorDto();
        syntaxValidationError.setObjectName("orderDto");
        syntaxValidationError.setField("customerName");
        syntaxValidationError.setMessage("must not be blank");

        expectedSyntaxError.setValidationErrors(List.of(syntaxValidationError));

        // 5. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSyntaxError),
                syntaxFailureResponse.getBody());

        // 6. Verify response headers
        Map<String, String> expectedSyntaxHeaders = TestHelper.getHeadersForIntegrationTestingNegative().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> syntacticHeaders = syntaxFailureResponse.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> syntaxFailureResponse.getHeaders().getFirst(h), (a, b) -> b,
                        TreeMap::new));

        Assertions.assertNotNull(syntacticHeaders.get("date"));
        syntacticHeaders.remove("date");

        Assertions.assertEquals(expectedSyntaxHeaders.toString(), syntacticHeaders.toString());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 3: SEMANTIC VALIDATION FAILURE
        // orderItems empty → 400 BAD REQUEST
        ////////////////////////////////////////////////////////////////////////

        // 1. Prepare semantically invalid order
        OrderDto semanticInvalidOrderDto = OrderTestHelper.createOrderDto();
        semanticInvalidOrderDto.setOrderItems(Collections.emptyList());

        ObjectNode semanticInvalidJson = objectMapper.valueToTree(semanticInvalidOrderDto);

        HttpEntity<String> semanticInvalidRequest = new HttpEntity<>(semanticInvalidJson.toString(), headers);

        // 2. Call CREATE API
        ResponseEntity<String> semanticFailureResponse = testRestTemplate.postForEntity("/api/orders",
                semanticInvalidRequest, String.class);

        // 3. Verify HTTP status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, semanticFailureResponse.getStatusCode());

        // 4. Build EXPECTED semantic validation error response
        ErrorResponseDto expectedSemanticError = new ErrorResponseDto();
        expectedSemanticError.setMessage("Request is not valid");
        expectedSemanticError.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto semanticValidationError = new ValidationErrorDto();
        semanticValidationError.setObjectName("orderDto");
        semanticValidationError.setField("orderItems");
        semanticValidationError.setMessage("Order must contain at least one item");

        expectedSemanticError.setValidationErrors(List.of(semanticValidationError));

        // 5. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSemanticError),
                semanticFailureResponse.getBody());

        // 6. Verify response headers
        Map<String, String> expectedSemanticHeaders = TestHelper.getHeadersForIntegrationTestingNegative().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> semanticHeaders = semanticFailureResponse.getHeaders().keySet().stream().collect(Collectors
                .toMap(h -> h, h -> semanticFailureResponse.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(semanticHeaders.get("date"));
        semanticHeaders.remove("date");

        Assertions.assertEquals(expectedSemanticHeaders.toString(), semanticHeaders.toString());
    }

    /**
     * Integration test for the UPDATE Order API.
     *
     * <p>
     * Verifies the following scenarios:
     * <ul>
     * <li>Successful update of an existing order (200 OK)</li>
     * <li>Syntax validation failure when mandatory fields are missing (400 Bad
     * Request)</li>
     * <li>Semantic validation failure when updating a non-existing order (400 Bad
     * Request)</li>
     * </ul>
     * </p>
     *
     * <p>
     * Ensures that update validations, persistence, and response mappings behave
     * correctly in an end-to-end flow.
     * </p>
     */
    @Test
    void testUpdate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // COMMON HEADERS SETUP
        ////////////////////////////////////////////////////////////////////////
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ////////////////////////////////////////////////////////////////////////
        // PRECONDITION: CREATE ORDER TO UPDATE
        ////////////////////////////////////////////////////////////////////////
        OrderDto originalOrderDto = OrderTestHelper.createOrderDto();
        ObjectNode createRequestJson = objectMapper.valueToTree(originalOrderDto);

        ResponseEntity<String> createResponse = testRestTemplate.postForEntity("/api/orders",
                new HttpEntity<>(createRequestJson.toString(), headers), String.class);

        Assertions.assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        ObjectNode createdJson = (ObjectNode) objectMapper.readTree(createResponse.getBody());

        UUID orderId = UUID.fromString(createdJson.get("id").asText());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 1: POSITIVE CASE
        // Valid update request → 200 OK
        ////////////////////////////////////////////////////////////////////////

        // 1. Prepare UPDATE request
        OrderDto updatedOrderDto = new OrderDto();
        updatedOrderDto.setCustomerName("Updated Customer");
        updatedOrderDto.setCustomerEmail("updated.customer@gmail.com");
        updatedOrderDto.setDeliveryAddress("Updated Address, City, Country");
        updatedOrderDto.setOrderStatus(originalOrderDto.getOrderStatus());
        updatedOrderDto.setTotalAmount(originalOrderDto.getTotalAmount());
        updatedOrderDto.setPaymentMethod(originalOrderDto.getPaymentMethod());
        updatedOrderDto.setNotes("Updated notes");
        updatedOrderDto.setOrderItems(originalOrderDto.getOrderItems());

        ObjectNode updateRequestJson = objectMapper.valueToTree(updatedOrderDto);

        // 2. Call UPDATE API
        ResponseEntity<String> response = testRestTemplate.exchange("/api/orders/" + orderId, HttpMethod.PUT,
                new HttpEntity<>(updateRequestJson.toString(), headers), String.class);

        // 3. Verify HTTP status
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // 4. Verify response body
        Assertions.assertNotNull(response.getBody());

        // 5. Parse ACTUAL response JSON
        ObjectNode actualJson = (ObjectNode) objectMapper.readTree(response.getBody());

        // 6. Build EXPECTED JSON from update DTO
        ObjectNode expectedJson = objectMapper.valueToTree(updatedOrderDto);

        // 7. Copy GENERATED root-level fields
        expectedJson.put("id", actualJson.get("id").asText());
        expectedJson.set("createdDateTime", actualJson.get("createdDateTime"));
        expectedJson.set("lastModifiedDateTime", actualJson.get("lastModifiedDateTime"));

        // 8. Normalize & copy GENERATED fields inside orderItems
        for (int i = 0; i < actualJson.withArray("orderItems").size(); i++) {

            ObjectNode actualItem = (ObjectNode) actualJson.withArray("orderItems").get(i);

            ObjectNode expectedItem = (ObjectNode) expectedJson.withArray("orderItems").get(i);

            expectedItem.put("id", actualItem.get("id").asText());
            expectedItem.set("createdDateTime", actualItem.get("createdDateTime"));
            expectedItem.set("lastModifiedDateTime", actualItem.get("lastModifiedDateTime"));
        }

        // 9. Assert FULL JSON equality (STRICT)
        JSONAssert.assertEquals(expectedJson.toString(), actualJson.toString(), true);

        // 10. Verify response headers
        Map<String, String> expectedHeaders = TestHelper.getHeadersForIntegrationTesting().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> actualHeaders = response.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> response.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(actualHeaders.get("date"));
        actualHeaders.remove("date");

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 2: SYNTAX VALIDATION FAILURE
        // Missing customerName → 400 BAD REQUEST
        ////////////////////////////////////////////////////////////////////////

        // 1. Prepare invalid UPDATE JSON (customerName missing)
        ObjectNode syntaxInvalidJson = objectMapper.createObjectNode();
        syntaxInvalidJson.put("customerEmail", updatedOrderDto.getCustomerEmail());
        syntaxInvalidJson.put("deliveryAddress", updatedOrderDto.getDeliveryAddress());
        syntaxInvalidJson.put("orderStatus", updatedOrderDto.getOrderStatus());
        syntaxInvalidJson.put("paymentMethod", updatedOrderDto.getPaymentMethod());
        syntaxInvalidJson.set("orderItems", objectMapper.valueToTree(updatedOrderDto.getOrderItems()));

        HttpEntity<String> syntaxInvalidRequest = new HttpEntity<>(syntaxInvalidJson.toString(), headers);

        // 2. Call UPDATE API
        ResponseEntity<String> syntaxFailureResponse = testRestTemplate.exchange("/api/orders/" + orderId,
                HttpMethod.PUT, syntaxInvalidRequest, String.class);

        // 3. Verify HTTP status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, syntaxFailureResponse.getStatusCode());

        // 4. Build EXPECTED syntactic error response
        ErrorResponseDto expectedSyntaxError = new ErrorResponseDto();
        expectedSyntaxError.setMessage("Request is not valid");
        expectedSyntaxError.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto syntaxValidationError = new ValidationErrorDto();
        syntaxValidationError.setObjectName("orderDto");
        syntaxValidationError.setField("customerName");
        syntaxValidationError.setMessage("must not be blank");

        expectedSyntaxError.setValidationErrors(List.of(syntaxValidationError));

        // 5. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSyntaxError),
                syntaxFailureResponse.getBody());

        // 6. Verify response headers
        Map<String, String> expectedSyntaxHeaders = TestHelper.getHeadersForIntegrationTestingNegative().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> syntaxHeaders = syntaxFailureResponse.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> syntaxFailureResponse.getHeaders().getFirst(h), (a, b) -> b,
                        TreeMap::new));

        Assertions.assertNotNull(syntaxHeaders.get("date"));
        syntaxHeaders.remove("date");

        Assertions.assertEquals(expectedSyntaxHeaders.toString(), syntaxHeaders.toString());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 3: SEMANTIC VALIDATION FAILURE
        // Updating non-existing order → 404 NOT FOUND
        ////////////////////////////////////////////////////////////////////////

        // 1. Prepare valid UPDATE JSON
        ObjectNode semanticInvalidJson = objectMapper.valueToTree(updatedOrderDto);

        // 2. Call UPDATE API with non-existing ID
        ResponseEntity<String> semanticFailureResponse = testRestTemplate.exchange("/api/orders/999999", HttpMethod.PUT,
                new HttpEntity<>(semanticInvalidJson.toString(), headers), String.class);

        // 3. Verify HTTP status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, semanticFailureResponse.getStatusCode());

        // 4. Build EXPECTED semantic error response
        ErrorResponseDto expectedSemanticError = new ErrorResponseDto();
        expectedSemanticError.setMessage("Request is not valid. Please check the validation errors for more details.");
        List<ValidationErrorDto> sementicValidationErrorDtoList = new ArrayList<ValidationErrorDto>();
        ValidationErrorDto sementicValidationErrorDto = new ValidationErrorDto();
        sementicValidationErrorDto.setObjectName("orderDto");
        sementicValidationErrorDto.setField("id");
        sementicValidationErrorDto.setMessage("Order with ID '999999' not found.");
        sementicValidationErrorDtoList.add(sementicValidationErrorDto);
        expectedSemanticError.setValidationErrors(sementicValidationErrorDtoList);

        // 5. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSemanticError),
                semanticFailureResponse.getBody());

        // 6. Verify response headers
        Map<String, String> expectedSemanticHeaders = TestHelper.getHeadersForIntegrationTestingNegative().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> semanticHeaders = semanticFailureResponse.getHeaders().keySet().stream().collect(Collectors
                .toMap(h -> h, h -> semanticFailureResponse.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(semanticHeaders.get("date"));
        semanticHeaders.remove("date");

        Assertions.assertEquals(expectedSemanticHeaders.toString(), semanticHeaders.toString());
    }

    /**
     * Integration test for the PATCH Order API.
     *
     * <p>
     * Verifies the following scenarios:
     * <ul>
     * <li>Successful partial update of an order (200 OK)</li>
     * <li>Syntactic validation failure for invalid patch input (400 Bad
     * Request)</li>
     * <li>Semantic validation failure for invalid order status transitions (400 Bad
     * Request)</li>
     * </ul>
     * </p>
     *
     * <p>
     * This test specifically validates the behavior of the
     * {@code OrderStatusTransitionValidator} in the patch validation chain.
     * </p>
     */
    @Test
    void testPatch() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // COMMON HEADERS SETUP
        ////////////////////////////////////////////////////////////////////////
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ////////////////////////////////////////////////////////////////////////
        // PRECONDITION: CREATE ORDER TO PATCH
        ////////////////////////////////////////////////////////////////////////
        OrderDto originalOrderDto = OrderTestHelper.createOrderDto();
        ObjectNode createRequestJson = objectMapper.valueToTree(originalOrderDto);

        ResponseEntity<String> createResponse = testRestTemplate.postForEntity("/api/orders",
                new HttpEntity<>(createRequestJson.toString(), headers), String.class);

        Assertions.assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        ObjectNode createdJson = (ObjectNode) objectMapper.readTree(createResponse.getBody());

        UUID orderId = UUID.fromString(createdJson.get("id").asText());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 1: POSITIVE CASE
        // Valid PATCH request (notes only) → 200 OK
        ////////////////////////////////////////////////////////////////////////

        // 1. Prepare PATCH request
        OrderDto patchedOrderDto = new OrderDto();
        patchedOrderDto.setNotes("Partially updated notes");

        ObjectNode patchRequestJson = objectMapper.valueToTree(patchedOrderDto);

        // 2. Call PATCH API
        ResponseEntity<String> response = testRestTemplate.exchange("/api/orders/" + orderId, HttpMethod.PATCH,
                new HttpEntity<>(patchRequestJson.toString(), headers), String.class);

        // 3. Verify HTTP status
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // 4. Verify response body
        Assertions.assertNotNull(response.getBody());

        // 5. Parse ACTUAL response JSON
        ObjectNode actualJson = (ObjectNode) objectMapper.readTree(response.getBody());

        // 6. Build EXPECTED JSON (original + patch)
        OrderDto expectedOrderDto = OrderTestHelper.createOrderDto();
        expectedOrderDto.setNotes("Partially updated notes");

        ObjectNode expectedJson = objectMapper.valueToTree(expectedOrderDto);

        // 7. Copy GENERATED root-level fields
        expectedJson.put("id", actualJson.get("id").asText());
        expectedJson.set("createdDateTime", actualJson.get("createdDateTime"));
        expectedJson.set("lastModifiedDateTime", actualJson.get("lastModifiedDateTime"));

        // 8. Normalize & copy GENERATED fields inside orderItems
        for (int i = 0; i < actualJson.withArray("orderItems").size(); i++) {

            ObjectNode actualItem = (ObjectNode) actualJson.withArray("orderItems").get(i);

            ObjectNode expectedItem = (ObjectNode) expectedJson.withArray("orderItems").get(i);

            expectedItem.put("id", actualItem.get("id").asText());
            expectedItem.set("createdDateTime", actualItem.get("createdDateTime"));
            expectedItem.set("lastModifiedDateTime", actualItem.get("lastModifiedDateTime"));
        }

        // 9. Assert FULL JSON equality (STRICT)
        JSONAssert.assertEquals(expectedJson.toString(), actualJson.toString(), true);

        // 10. Verify response headers
        Map<String, String> expectedHeaders = TestHelper.getHeadersForIntegrationTesting().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> actualHeaders = response.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> response.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(actualHeaders.get("date"));
        actualHeaders.remove("date");

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 2: SYNTAX VALIDATION FAILURE
        // Invalid PATCH (orderStatus empty string) → 400 BAD REQUEST
        ////////////////////////////////////////////////////////////////////////

        // 1. Prepare invalid PATCH JSON
        ObjectNode syntaxInvalidJson = objectMapper.createObjectNode();
        syntaxInvalidJson.put("orderStatus", "INVALID");

        HttpEntity<String> syntaxInvalidRequest = new HttpEntity<>(syntaxInvalidJson.toString(), headers);

        // 2. Call PATCH API
        ResponseEntity<String> syntaxFailureResponse = testRestTemplate.exchange("/api/orders/" + orderId,
                HttpMethod.PATCH, syntaxInvalidRequest, String.class);

        // 3. Verify HTTP status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, syntaxFailureResponse.getStatusCode());

        // 4. Build EXPECTED syntax error response
        ErrorResponseDto expectedSyntaxError = new ErrorResponseDto();
        expectedSyntaxError.setMessage("Request is not valid");
        expectedSyntaxError.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto syntaxValidationError1 = new ValidationErrorDto();
        syntaxValidationError1.setObjectName("orderDto");
        syntaxValidationError1.setField("orderStatus");
        syntaxValidationError1.setMessage("Invalid order status");

        expectedSyntaxError.setValidationErrors(List.of(syntaxValidationError1));

        // 5. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSyntaxError),
                syntaxFailureResponse.getBody());

        // 6. Verify response headers
        Map<String, String> expectedSyntaxHeaders = TestHelper.getHeadersForIntegrationTestingNegative().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> syntaxHeaders = syntaxFailureResponse.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> syntaxFailureResponse.getHeaders().getFirst(h), (a, b) -> b,
                        TreeMap::new));

        Assertions.assertNotNull(syntaxHeaders.get("date"));
        syntaxHeaders.remove("date");

        Assertions.assertEquals(expectedSyntaxHeaders.toString(), syntaxHeaders.toString());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 3: SEMANTIC VALIDATION FAILURE
        // Invalid order status transition → 400 BAD REQUEST
        ////////////////////////////////////////////////////////////////////////

        // 1. Prepare PATCH JSON with INVALID transition (PENDING → DELIVERED)
        OrderDto semanticInvalidOrderDto = new OrderDto();
        semanticInvalidOrderDto.setOrderStatus("DELIVERED");

        ObjectNode semanticInvalidJson = objectMapper.valueToTree(semanticInvalidOrderDto);

        // 2. Call PATCH API
        ResponseEntity<String> semanticFailureResponse = testRestTemplate.exchange("/api/orders/" + orderId,
                HttpMethod.PATCH, new HttpEntity<>(semanticInvalidJson.toString(), headers), String.class);

        // 3. Verify HTTP status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, semanticFailureResponse.getStatusCode());

        // 4. Build EXPECTED semantic validation error response
        ErrorResponseDto expectedSemanticError = new ErrorResponseDto();
        expectedSemanticError.setMessage("Request is not valid. Please check the validation errors for more details.");

        ValidationErrorDto semanticValidationError = new ValidationErrorDto();
        semanticValidationError.setObjectName("orderDto");
        semanticValidationError.setField("orderStatus");
        semanticValidationError.setMessage("Invalid status transition from 'PENDING' to 'DELIVERED'.");

        expectedSemanticError.setValidationErrors(List.of(semanticValidationError));

        // 5. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSemanticError),
                semanticFailureResponse.getBody());

        // 6. Verify response headers
        Map<String, String> expectedSemanticHeaders = TestHelper.getHeadersForIntegrationTestingNegative().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> semanticHeaders = semanticFailureResponse.getHeaders().keySet().stream().collect(Collectors
                .toMap(h -> h, h -> semanticFailureResponse.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(semanticHeaders.get("date"));
        semanticHeaders.remove("date");

        Assertions.assertEquals(expectedSemanticHeaders.toString(), semanticHeaders.toString());
    }

    /**
     * Integration test for the GET Order by ID API.
     *
     * <p>
     * Verifies the following scenarios:
     * <ul>
     * <li>Successful retrieval of an existing order (200 OK)</li>
     * <li>Syntax validation failure for invalid path variable type (400 Bad
     * Request)</li>
     * <li>Semantic validation failure when the order does not exist (404 Not
     * Found)</li>
     * </ul>
     * </p>
     *
     * <p>
     * Confirms correct behavior of request binding, validation chains, and
     * exception handling.
     * </p>
     */
    @Test
    void testGet() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // COMMON HEADERS SETUP
        ////////////////////////////////////////////////////////////////////////
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ////////////////////////////////////////////////////////////////////////
        // PRECONDITION: CREATE ORDER TO FETCH
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        ObjectNode createRequestJson = objectMapper.valueToTree(orderDto);

        ResponseEntity<String> createResponse = testRestTemplate.postForEntity("/api/orders",
                new HttpEntity<>(createRequestJson.toString(), headers), String.class);

        Assertions.assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        ObjectNode createdJson = (ObjectNode) objectMapper.readTree(createResponse.getBody());

        UUID orderId = UUID.fromString(createdJson.get("id").asText());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 1: POSITIVE CASE
        // Existing order → 200 OK
        ////////////////////////////////////////////////////////////////////////

        // 1. Call GET API
        ResponseEntity<String> response = testRestTemplate.getForEntity("/api/orders/" + orderId, String.class);

        // 2. Verify HTTP status
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // 3. Verify response body
        Assertions.assertNotNull(response.getBody());

        // 4. Parse ACTUAL response JSON
        ObjectNode actualJson = (ObjectNode) objectMapper.readTree(response.getBody());

        // 5. Build EXPECTED JSON from original DTO
        ObjectNode expectedJson = objectMapper.valueToTree(orderDto);

        // 6. Copy GENERATED root-level fields
        expectedJson.put("id", actualJson.get("id").asText());
        expectedJson.set("createdDateTime", actualJson.get("createdDateTime"));
        expectedJson.set("lastModifiedDateTime", actualJson.get("lastModifiedDateTime"));

        // 7. Normalize & copy GENERATED fields inside orderItems
        for (int i = 0; i < actualJson.withArray("orderItems").size(); i++) {

            ObjectNode actualItem = (ObjectNode) actualJson.withArray("orderItems").get(i);

            ObjectNode expectedItem = (ObjectNode) expectedJson.withArray("orderItems").get(i);

            expectedItem.put("id", actualItem.get("id").asText());
            expectedItem.set("createdDateTime", actualItem.get("createdDateTime"));
            expectedItem.set("lastModifiedDateTime", actualItem.get("lastModifiedDateTime"));
        }

        // 8. Assert FULL JSON equality
        JSONAssert.assertEquals(expectedJson.toString(), actualJson.toString(), true);

        // 9. Verify response headers
        Map<String, String> expectedHeaders = TestHelper.getHeadersForIntegrationTesting().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> actualHeaders = response.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> response.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(actualHeaders.get("date"));
        actualHeaders.remove("date");

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 2: SYNTAX VALIDATION FAILURE
        // Invalid path variable type → 400 BAD REQUEST
        ////////////////////////////////////////////////////////////////////////

        // 1. Call GET API with invalid ID type
        ResponseEntity<String> syntaxFailureResponse = testRestTemplate.getForEntity("/api/orders/abc",
                String.class);

        // 2. Verify HTTP status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, syntaxFailureResponse.getStatusCode());

        // 3. Build EXPECTED syntax error response
        ErrorResponseDto expectedSyntaxError = new ErrorResponseDto();
        expectedSyntaxError.setMessage("Request is not valid");
        expectedSyntaxError.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto syntaxValidationError = new ValidationErrorDto();
        syntaxValidationError.setField("id");
        syntaxValidationError.setMessage("Invalid value 'abc' for parameter 'id'. Expected type is 'UUID'");

        expectedSyntaxError.setValidationErrors(List.of(syntaxValidationError));

        // 4. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSyntaxError),
                syntaxFailureResponse.getBody());

        // 5. Verify response headers
        Map<String, String> expectedSyntaxHeaders = TestHelper.getHeadersForIntegrationTestingNegative().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> syntaxHeaders = syntaxFailureResponse.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> syntaxFailureResponse.getHeaders().getFirst(h), (a, b) -> b,
                        TreeMap::new));

        Assertions.assertNotNull(syntaxHeaders.get("date"));
        syntaxHeaders.remove("date");

        Assertions.assertEquals(expectedSyntaxHeaders.toString(), syntaxHeaders.toString());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 3: SEMANTIC VALIDATION FAILURE
        // Order does not exist → 400 BAD REQUEST (OrderExistenceValidator)
        ////////////////////////////////////////////////////////////////////////

        // 1. Call GET API with non-existing ID
        ResponseEntity<String> semanticFailureResponse = testRestTemplate.getForEntity("/api/orders/999999",
                String.class);

        // 2. Verify HTTP status
        Assertions.assertEquals(HttpStatus.NOT_FOUND, semanticFailureResponse.getStatusCode());

        // 3. Build EXPECTED semantic error response
        ErrorResponseDto expectedSemanticError = new ErrorResponseDto();
        expectedSemanticError.setMessage("Resource not found");
        expectedSemanticError.setDescription("Order with '999999' does not exist");

        // 4. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSemanticError),
                semanticFailureResponse.getBody());

        // 5. Verify response headers
        Map<String, String> expectedSementicHeaders = TestHelper.getHeadersForIntegrationTesting().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> semanticHeaders = semanticFailureResponse.getHeaders().keySet().stream().collect(Collectors
                .toMap(h -> h, h -> semanticFailureResponse.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(semanticHeaders.get("date"));
        semanticHeaders.remove("date");

        Assertions.assertEquals(expectedSementicHeaders.toString(), semanticHeaders.toString());
    }

    /**
     * Integration test for the DELETE Order API.
     *
     * <p>
     * Verifies the following scenarios:
     * <ul>
     * <li>Successful deletion (cancellation) of an order (204 No Content)</li>
     * <li>Semantic validation failure when attempting to delete a non-existing
     * order (400 Bad Request)</li>
     * </ul>
     * </p>
     *
     * <p>
     * Ensures that order deletion rules enforced by {@code OrderExistenceValidator}
     * and {@code OrderDeletionStatusValidator} behave correctly.
     * </p>
     */
    @Test
    void testDelete() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // COMMON HEADERS SETUP
        ////////////////////////////////////////////////////////////////////////
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ////////////////////////////////////////////////////////////////////////
        // PRECONDITION: CREATE ORDER TO DELETE
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        ObjectNode createRequestJson = objectMapper.valueToTree(orderDto);

        ResponseEntity<String> createResponse = testRestTemplate.postForEntity("/api/orders",
                new HttpEntity<>(createRequestJson.toString(), headers), String.class);

        Assertions.assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        ObjectNode createdJson = (ObjectNode) objectMapper.readTree(createResponse.getBody());

        UUID orderId = UUID.fromString(createdJson.get("id").asText());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 1: POSITIVE CASE
        // Order exists & status is cancellable → 204 NO CONTENT
        ////////////////////////////////////////////////////////////////////////

        // 1. Call DELETE API
        ResponseEntity<Void> response = testRestTemplate.exchange("/api/orders/" + orderId, HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        // 2. Verify HTTP status
        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(),
                "HTTP status should be 204 No Content");

        // 3. Verify response body
        Assertions.assertNull(response.getBody(), "Response body must be null for 204 No Content");

        // 4. Verify response headers
        Map<String, String> expectedHeaders = TestHelper.getHeadersForDeleteMethodOfIntegrationTesting().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> actualHeaders = response.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> response.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(actualHeaders.get("date"));
        actualHeaders.remove("date");

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers do not match");

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 2: SEMANTIC VALIDATION FAILURE
        // Order does NOT exist → Validation failure
        // (OrderExistenceValidator)
        ////////////////////////////////////////////////////////////////////////

        // 1. Call DELETE API with non-existing ID
        ResponseEntity<String> semanticFailureResponse = testRestTemplate.exchange("/api/orders/999999",
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);

        // 2. Verify HTTP status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, semanticFailureResponse.getStatusCode());

        // 3. Build EXPECTED semantic error response
        ErrorResponseDto expectedSemanticError = new ErrorResponseDto();
        expectedSemanticError.setMessage("Request is not valid. Please check the validation errors for more details.");

        ValidationErrorDto semanticValidationError = new ValidationErrorDto();
        semanticValidationError.setObjectName("orderDto");
        semanticValidationError.setField("id");
        semanticValidationError.setMessage("Order with ID '999999' not found.");

        expectedSemanticError.setValidationErrors(List.of(semanticValidationError));

        // 4. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSemanticError),
                semanticFailureResponse.getBody());

        // 5. Verify response headers
        Map<String, String> expectedSemanticHeaders = TestHelper.getHeadersForIntegrationTestingNegative().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> semanticHeaders = semanticFailureResponse.getHeaders().keySet().stream().collect(Collectors
                .toMap(h -> h, h -> semanticFailureResponse.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(semanticHeaders.get("date"));
        semanticHeaders.remove("date");

        Assertions.assertEquals(expectedSemanticHeaders.toString(), semanticHeaders.toString());
    }

    /**
     * Integration test for the GET ALL Orders API.
     *
     * <p>
     * Verifies the following scenarios:
     * <ul>
     * <li>Successful retrieval of paginated orders with valid parameters (200
     * OK)</li>
     * <li>Semantic validation failure for invalid sort parameter (400 Bad
     * Request)</li>
     * </ul>
     * </p>
     *
     * <p>
     * This test validates the behavior of {@code PaginationParameterValidator}
     * executed via the order validation service.
     * </p>
     */
    @Test
    void testGetAll() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // COMMON HEADERS SETUP
        ////////////////////////////////////////////////////////////////////////
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ////////////////////////////////////////////////////////////////////////
        // PRECONDITION: CREATE ONE ORDER
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        ObjectNode createRequestJson = objectMapper.valueToTree(orderDto);

        ResponseEntity<String> createResponse = testRestTemplate.postForEntity("/api/orders",
                new HttpEntity<>(createRequestJson.toString(), headers), String.class);

        Assertions.assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 1: POSITIVE CASE
        // Valid pagination & sorting → 200 OK
        ////////////////////////////////////////////////////////////////////////

        // 1. Call GET ALL API with valid pageable (page=0, size=1, valid sort)
        ResponseEntity<String> response = testRestTemplate
                .getForEntity("/api/orders?page=0&size=1&sort=createdDateTime,asc", String.class);

        // 2. Verify HTTP status
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // 3. Verify response body
        Assertions.assertNotNull(response.getBody());

        // 4. Parse ACTUAL response JSON
        ObjectNode actualJson = (ObjectNode) objectMapper.readTree(response.getBody());

        // 5. Verify page metadata
        Assertions.assertEquals(1, actualJson.get("page").get("totalElements").asInt());
        Assertions.assertEquals(1, actualJson.get("page").get("size").asInt());
        Assertions.assertEquals(0, actualJson.get("page").get("number").asInt());

        // 6. Verify content size
        Assertions.assertEquals(1, actualJson.withArray("content").size());

        // 7. Extract actual order
        ObjectNode actualOrderJson = (ObjectNode) actualJson.withArray("content").get(0);

        // 8. Build EXPECTED order JSON
        ObjectNode expectedOrderJson = objectMapper.valueToTree(orderDto);

        // 9. Copy GENERATED root-level fields
        expectedOrderJson.put("id", actualOrderJson.get("id").asText());
        expectedOrderJson.set("createdDateTime", actualOrderJson.get("createdDateTime"));
        expectedOrderJson.set("lastModifiedDateTime", actualOrderJson.get("lastModifiedDateTime"));

        // 10. Normalize & copy GENERATED fields inside orderItems
        for (int i = 0; i < actualOrderJson.withArray("orderItems").size(); i++) {

            ObjectNode actualItem = (ObjectNode) actualOrderJson.withArray("orderItems").get(i);

            ObjectNode expectedItem = (ObjectNode) expectedOrderJson.withArray("orderItems").get(i);

            expectedItem.put("id", actualItem.get("id").asText());
            expectedItem.set("createdDateTime", actualItem.get("createdDateTime"));
            expectedItem.set("lastModifiedDateTime", actualItem.get("lastModifiedDateTime"));
        }

        // 11. Assert FULL JSON equality (STRICT)
        JSONAssert.assertEquals(expectedOrderJson.toString(), actualOrderJson.toString(), true);

        // 12. Verify response headers
        Map<String, String> expectedHeaders = TestHelper.getHeadersForIntegrationTesting().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> actualHeaders = response.getHeaders().keySet().stream()
                .collect(Collectors.toMap(h -> h, h -> response.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(actualHeaders.get("date"));
        actualHeaders.remove("date");

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString());

        ////////////////////////////////////////////////////////////////////////
        // SCENARIO 2: SEMANTIC VALIDATION FAILURE
        // Invalid sort field → 400 BAD REQUEST
        ////////////////////////////////////////////////////////////////////////

        // 1. Call GET ALL API with INVALID sort field
        ResponseEntity<String> semanticFailureResponse = testRestTemplate
                .getForEntity("/api/orders?page=0&size=1&sort=invalidField,asc", String.class);

        // 2. Verify HTTP status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, semanticFailureResponse.getStatusCode());

        // 3. Build EXPECTED semantic error response
        ErrorResponseDto expectedSemanticError = new ErrorResponseDto();
        expectedSemanticError.setMessage("Request is not valid. Please check the validation errors for more details.");

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setObjectName("orderDto");
        validationErrorDto.setField("sort");
        validationErrorDto.setMessage(
                "Sorting by 'invalidField' is not allowed. Allowed fields are: '[createdDateTime, customerEmail, customerName, id, lastModifiedDateTime, orderDate, orderStatus, paymentMethod, totalAmount]'");

        expectedSemanticError.setValidationErrors(List.of(validationErrorDto));

        // 4. Verify response body
        Assertions.assertEquals(objectMapper.writeValueAsString(expectedSemanticError),
                semanticFailureResponse.getBody());

        // 5. Verify response headers
        Map<String, String> expectedSemanticHeaders = TestHelper.getHeadersForIntegrationTestingNegative().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> b, TreeMap::new));

        Map<String, String> semanticHeaders = semanticFailureResponse.getHeaders().keySet().stream().collect(Collectors
                .toMap(h -> h, h -> semanticFailureResponse.getHeaders().getFirst(h), (a, b) -> b, TreeMap::new));

        Assertions.assertNotNull(semanticHeaders.get("date"));
        semanticHeaders.remove("date");

        Assertions.assertEquals(expectedSemanticHeaders.toString(), semanticHeaders.toString());
    }

}
