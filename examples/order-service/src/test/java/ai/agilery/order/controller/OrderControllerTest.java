package ai.agilery.order.controller;

import java.util.UUID;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.agilery.order.dto.ErrorResponseDto;
import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.exceptions.ProcessingException;
import ai.agilery.order.exceptions.ResourceNotFoundException;
import ai.agilery.order.exceptions.ValidationException;
import ai.agilery.order.service.OrderService;
import ai.agilery.order.utils.OrderTestHelper;
import ai.agilery.order.utils.TestHelper;
import ai.agilery.order.validation.impl.service.OrderValidationService;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderValidationService orderValidationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Tests the create order API for a valid request.
     *
     * <p>
     * Verifies that when a valid {@link OrderDto} is submitted, the order is
     * created successfully and the API returns HTTP status {@code 201 (Created)}.
     * </p>
     */
    @Test
    void testSuccessForCreate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();

        Mockito.doNothing().when(orderValidationService).onCreate(Mockito.any(OrderDto.class));
        Mockito.when(orderService.create(Mockito.any(OrderDto.class))).thenReturn(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.CREATED.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();
        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        String expected = objectMapper.writeValueAsString(orderDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Response body does not match for successful order creation");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onCreate(orderDto);
        Mockito.verify(orderService, Mockito.times(1)).create(orderDto);
    }

    /**
     * Tests the create order API when the customer name is invalid.
     *
     * <p>
     * Verifies that when the customer name contains only a single character, the
     * request fails validation and the API returns HTTP status
     * {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSyntaxValidationForCreate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        orderDto.setCustomerName("A"); // invalid: size < 2

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();
        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid");
        expectedErrorResponseDto.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setObjectName("orderDto");
        validationErrorDto.setField("customerName");
        validationErrorDto.setMessage("Customer name must be between 2 and 200 characters");

        expectedErrorResponseDto.setValidationErrors(List.of(validationErrorDto));

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for invalid customer name length");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderService, Mockito.never()).create(Mockito.any());
        Mockito.verify(orderValidationService, Mockito.never()).onCreate(Mockito.any());
    }

    /**
     * Tests the create order API when the total amount does not match the order
     * items.
     *
     * <p>
     * Verifies that when the total amount is inconsistent with the sum of order
     * item subtotals, the semantic validation fails and the API returns HTTP status
     * {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSemanticValidationForCreate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();

        // Force semantic validation failure (items total ≠ totalAmount)
        orderDto.setTotalAmount(new BigDecimal("250.00"));

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setObjectName("orderDto");
        validationErrorDto.setField("totalAmount");
        validationErrorDto.setMessage("Total amount (250.00) does not match the sum of order item subtotals (300.00).");

        List<ValidationErrorDto> validationErrors = List.of(validationErrorDto);

        ValidationException validationException = new ValidationException(validationErrors);

        Mockito.doThrow(validationException).when(orderValidationService).onCreate(Mockito.any(OrderDto.class));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();
        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid. Please check the validation errors for more details.");
        expectedErrorResponseDto.setValidationErrors(validationErrors);

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for semantic validation failure");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onCreate(orderDto);
        Mockito.verify(orderService, Mockito.never()).create(Mockito.any());
    }

    /**
     * Tests the create order API when an internal server error occurs.
     *
     * <p>
     * Verifies that when a {@link ProcessingException} is thrown during order
     * creation, the API returns HTTP status {@code 500 (Internal Server Error)}
     * with the appropriate error response.
     * </p>
     */
    @Test
    void testInternalServerErrorForCreate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();

        ProcessingException processingException = new ProcessingException("Database connection timeout");

        Mockito.doNothing().when(orderValidationService).onCreate(Mockito.any(OrderDto.class));
        Mockito.when(orderService.create(Mockito.any(OrderDto.class))).thenThrow(processingException);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isInternalServerError())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus(),
                "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Database connection timeout");
        expectedErrorResponseDto
        .setDescription("An unexpected error has occurred on server. Please try again in 5 to 10 minutes. "
                + "If still problem persists contact our support team at support@agilery.ai");

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for server side ProcessingException");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onCreate(orderDto);
        Mockito.verify(orderService, Mockito.times(1)).create(orderDto);
    }

    /**
     * Tests the update order API for a valid request.
     *
     * <p>
     * Verifies that when a valid {@link OrderDto} is provided to update an existing
     * order, the API returns HTTP status {@code 200 (OK)} with the updated order
     * details.
     * </p>
     */
    @Test
    void testSuccessForUpdate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        orderDto.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        Mockito.doNothing().when(orderValidationService).onUpdate(orderDto);
        Mockito.when(orderService.update(Mockito.any(OrderDto.class))).thenReturn(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.put("/api/orders/{id}", orderDto.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        String expected = objectMapper.writeValueAsString(orderDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Response body does not match for successful order update");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onUpdate(orderDto);
        Mockito.verify(orderService, Mockito.times(1)).update(orderDto);
    }

    /**
     * Tests the update order API when the customer email is invalid.
     *
     * <p>
     * Verifies that when an invalid customer email format is provided, the request
     * fails validation and the API returns HTTP status {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSyntaxValidationForUpdate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        orderDto.setCustomerEmail("invalid-email-format");
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.put("/api/orders/{id}", orderId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid");
        expectedErrorResponseDto.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setObjectName("orderDto");
        validationErrorDto.setField("customerEmail");
        validationErrorDto.setMessage("Customer email must be a valid email address");

        expectedErrorResponseDto.setValidationErrors(List.of(validationErrorDto));

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for invalid customer email format");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify no interaction with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.never()).onUpdate(Mockito.any());
        Mockito.verify(orderService, Mockito.never()).update(Mockito.any());
    }

    /**
     * Tests the update order API when the total amount does not match the order
     * items.
     *
     * <p>
     * Verifies that when the total amount is inconsistent with the sum of order
     * item subtotals, the semantic validation fails and the API returns HTTP status
     * {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSemanticValidationForUpdate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        orderDto.setTotalAmount(new BigDecimal("250.00"));

        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setObjectName("orderDto");
        validationErrorDto.setField("totalAmount");
        validationErrorDto.setMessage("Total amount (250.00) does not match the sum of order item subtotals (300.00).");

        List<ValidationErrorDto> validationErrors = List.of(validationErrorDto);

        ValidationException validationException = new ValidationException(validationErrors);

        Mockito.doThrow(validationException).when(orderValidationService).onUpdate(Mockito.any(OrderDto.class));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.put("/api/orders/{id}", orderId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid. Please check the validation errors for more details.");
        expectedErrorResponseDto.setValidationErrors(validationErrors);

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for semantic validation failure");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onUpdate(Mockito.any(OrderDto.class));
        Mockito.verify(orderService, Mockito.never()).update(Mockito.any(OrderDto.class));
    }

    /**
     * Tests the update order API when an internal server error occurs.
     *
     * <p>
     * Verifies that when a {@link ProcessingException} is thrown during order
     * update, the API returns HTTP status {@code 500 (Internal Server Error)} with
     * the appropriate error response.
     * </p>
     */
    @Test
    void testInternalServerErrorForUpdate() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        ProcessingException processingException = new ProcessingException("Database failure while updating order");

        Mockito.doNothing().when(orderValidationService).onUpdate(Mockito.any(OrderDto.class));
        Mockito.when(orderService.update(Mockito.any(OrderDto.class))).thenThrow(processingException);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.put("/api/orders/{id}", orderId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isInternalServerError())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus(),
                "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Database failure while updating order");
        expectedErrorResponseDto
        .setDescription("An unexpected error has occurred on server. Please try again in 5 to 10 minutes. "
                + "If still problem persists contact our support team at support@agilery.ai");

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for server side ProcessingException during update");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onUpdate(Mockito.any(OrderDto.class));
        Mockito.verify(orderService, Mockito.times(1)).update(Mockito.any(OrderDto.class));
    }

    /**
     * Tests the patch order API for a valid request.
     *
     * <p>
     * Verifies that when valid partial order details are provided, the API
     * successfully updates the order and returns HTTP status {@code 200 (OK)}.
     * </p>
     */
    @Test
    void testSuccessForPatch() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDtoForPatch();
        orderDto.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        Mockito.doNothing().when(orderValidationService).onPatch(orderDto);
        Mockito.when(orderService.patch(Mockito.any(OrderDto.class))).thenReturn(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.patch("/api/orders/{id}", orderDto.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        String expected = objectMapper.writeValueAsString(orderDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Response body does not match for successful order patch");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onPatch(orderDto);
        Mockito.verify(orderService, Mockito.times(1)).patch(orderDto);
    }

    /**
     * Tests the patch order API when the order status is invalid.
     *
     * <p>
     * Verifies that when an invalid order status value is provided, the request
     * fails validation and the API returns HTTP status {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSyntaxValidationForPatch() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        orderDto.setOrderStatus("INVALID_STATUS");

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.patch("/api/orders/{id}", orderId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid");
        expectedErrorResponseDto.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setObjectName("orderDto");
        validationErrorDto.setField("orderStatus");
        validationErrorDto.setMessage("Invalid order status");

        expectedErrorResponseDto.setValidationErrors(List.of(validationErrorDto));

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for invalid orderStatus on patch");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify no interaction with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.never()).onPatch(Mockito.any());
        Mockito.verify(orderService, Mockito.never()).patch(Mockito.any());
    }

    /**
     * Tests the patch order API when the order does not exist.
     *
     * <p>
     * Verifies that when an attempt is made to patch an order that does not exist,
     * the semantic validation fails and the API returns HTTP status
     * {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSemanticValidationForPatch() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDto();
        orderDto.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setObjectName("orderDto");
        validationErrorDto.setField("id");
        validationErrorDto.setMessage("Order with ID '1' not found.");

        List<ValidationErrorDto> validationErrors = List.of(validationErrorDto);

        ValidationException validationException = new ValidationException(validationErrors);

        Mockito.doThrow(validationException).when(orderValidationService).onPatch(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.patch("/api/orders/{id}", orderDto.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid. Please check the validation errors for more details.");
        expectedErrorResponseDto.setValidationErrors(validationErrors);

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for order existence semantic validation failure");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onPatch(orderDto);
        Mockito.verify(orderService, Mockito.never()).patch(orderDto);
    }

    /**
     * Tests the patch order API when an internal server error occurs.
     *
     * <p>
     * Verifies that when a {@link ProcessingException} is thrown during order
     * patch, the API returns HTTP status {@code 500 (Internal Server Error)} with
     * the appropriate error response.
     * </p>
     */
    @Test
    void testInternalServerErrorForPatch() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        OrderDto orderDto = OrderTestHelper.createOrderDtoForPatch();

        ProcessingException processingException = new ProcessingException("Database failure while patching order");

        Mockito.doNothing().when(orderValidationService).onPatch(orderDto);
        Mockito.when(orderService.patch(Mockito.any(OrderDto.class))).thenThrow(processingException);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.patch("/api/orders/{id}", orderDto.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isInternalServerError())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus(),
                "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Database failure while patching order");
        expectedErrorResponseDto.setDescription(
                "An unexpected error has occurred on server. Please try again in 5 to 10 minutes. If still problem persists contact our support team at support@agilery.ai");

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for server side ProcessingException during patch");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onPatch(orderDto);
        Mockito.verify(orderService, Mockito.times(1)).patch(orderDto);
    }

    /**
     * Tests the get order API for a valid order ID.
     *
     * <p>
     * Verifies that when a valid order ID is provided, the API retrieves the order
     * successfully and returns HTTP status {@code 200 (OK)} with the order details.
     * </p>
     */
    @Test
    void testSuccessForGet() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        OrderDto orderDto = OrderTestHelper.createOrderDto();

        Mockito.doNothing().when(orderValidationService).onGet(orderId);
        Mockito.when(orderService.get(orderId)).thenReturn(orderDto);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/api/orders/{id}", orderId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        String expected = objectMapper.writeValueAsString(orderDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Response body does not match for successful get order");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onGet(orderId);
        Mockito.verify(orderService, Mockito.times(1)).get(orderId);
    }

    /**
     * Tests the get order API when the order ID is non-numeric.
     *
     * <p>
     * Verifies that when a non-numeric order ID is provided, the request fails
     * during path variable binding and the API returns HTTP status
     * {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSyntaxValidationForGet() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        String invalidOrderId = "abc";

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/orders/{id}", invalidOrderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid");
        expectedErrorResponseDto.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setField("id");
        validationErrorDto.setMessage("Invalid value 'abc' for parameter 'id'. Expected type is 'UUID'");

        expectedErrorResponseDto.setValidationErrors(List.of(validationErrorDto));

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for invalid path variable id");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify no interaction with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.never()).onGet(Mockito.any());
        Mockito.verify(orderService, Mockito.never()).get(Mockito.any());
    }

    /**
     * Tests the get order API when the order does not exist.
     *
     * <p>
     * Verifies that when a valid order ID is provided but no order is found, the
     * API returns HTTP status {@code 404 (Not Found)} with an appropriate resource
     * not found error response.
     * </p>
     */
    @Test
    void testSemanticValidationForGet() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ResourceNotFoundException resourceNotFoundException = new ResourceNotFoundException(
                "Order with '" + orderId + "' does not exist");

        Mockito.doNothing().when(orderValidationService).onGet(orderId);
        Mockito.when(orderService.get(orderId)).thenThrow(resourceNotFoundException);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/api/orders/{id}", orderId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus(), "HTTP status not matched");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Resource not found");
        expectedErrorResponseDto.setDescription("Order with '" + orderId + "' does not exist");

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected, actual, "Error response does not match for order not found");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify no interaction with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onGet(orderId);
        Mockito.verify(orderService, Mockito.times(1)).get(orderId);
    }

    /**
     * Tests the get order API when an unexpected server error occurs.
     *
     * <p>
     * Verifies that when an unexpected {@link RuntimeException} is thrown during
     * order retrieval, the API returns HTTP status
     * {@code 500 (Internal Server Error)}.
     * </p>
     */
    @Test
    void testInternalServerErrorForGet() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        RuntimeException runtimeException = new RuntimeException("Database connection failure");

        Mockito.doNothing().when(orderValidationService).onGet(orderId);
        Mockito.when(orderService.get(orderId)).thenThrow(runtimeException);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/api/orders/{id}", orderId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isInternalServerError())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus(),
                "HTTP status not matched");

        // Verify response body (handled by generic Exception handler)
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Unexpected error");
        expectedErrorResponseDto
        .setDescription("An unexpected error has occurred on server. Please try after 5 to 10 minutes. "
                + "If the problem persists contact our support team at support@agilery.ai");

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for unexpected server error");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onGet(orderId);
        Mockito.verify(orderService, Mockito.times(1)).get(orderId);
    }

    /**
     * Tests the delete order API for a valid order ID.
     *
     * <p>
     * Verifies that when a valid order ID is provided, the order is cancelled
     * successfully and the API returns HTTP status {@code 204 (No Content)}.
     * </p>
     */
    @Test
    void testSuccessForDelete() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Mockito.doNothing().when(orderValidationService).onDelete(orderId);
        Mockito.doNothing().when(orderService).delete(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.delete("/api/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus(), "HTTP status not matched");

        // Verify response body is empty
        Assertions.assertTrue(response.getContentAsString().isEmpty(),
                "Response body should be empty for delete operation");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingDeleteMethodOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onDelete(orderId);
        Mockito.verify(orderService, Mockito.times(1)).delete(orderId);
    }

    /**
     * Tests the delete order API when the order ID is non-numeric.
     *
     * <p>
     * Verifies that when a non-numeric order ID is provided, the request fails
     * during path variable binding and the API returns HTTP status
     * {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSyntaxValidationForDelete() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        String invalidOrderId = "abc";

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.delete("/api/orders/{id}", invalidOrderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid");
        expectedErrorResponseDto.setDescription(
                "The request contains invalid or missing fields. Please check the validation errors for more details.");

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setField("id");
        validationErrorDto.setMessage("Invalid value 'abc' for parameter 'id'. Expected type is 'UUID'");

        expectedErrorResponseDto.setValidationErrors(List.of(validationErrorDto));

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for invalid path variable id in delete API");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify no interaction with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.never()).onDelete(Mockito.any());
        Mockito.verify(orderService, Mockito.never()).delete(Mockito.any());
    }

    /**
     * Tests the delete order API when the order status does not allow cancellation.
     *
     * <p>
     * Verifies that when an attempt is made to cancel an order whose status is not
     * {@code PENDING} or {@code CONFIRMED}, the semantic validation fails and the
     * API returns HTTP status {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSemanticValidationForDelete() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setObjectName("orderId");
        validationErrorDto.setField("orderStatus");
        validationErrorDto.setMessage(
                "Order cannot be cancelled in 'CANCELLED' status. Only PENDING or CONFIRMED orders can be cancelled.");

        List<ValidationErrorDto> validationErrors = List.of(validationErrorDto);

        ValidationException validationException = new ValidationException(validationErrors);

        Mockito.doThrow(validationException).when(orderValidationService).onDelete(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.delete("/api/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid. Please check the validation errors for more details.");
        expectedErrorResponseDto.setValidationErrors(validationErrors);

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for semantic validation failure during delete");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onDelete(orderId);
        Mockito.verify(orderService, Mockito.never()).delete(Mockito.any());
    }

    /**
     * Tests the delete order API when an unexpected server error occurs.
     *
     * <p>
     * Verifies that when an unexpected {@link RuntimeException} is thrown during
     * order cancellation, the API returns HTTP status
     * {@code 500 (Internal Server Error)}.
     * </p>
     */
    @Test
    void testInternalServerErrorForDelete() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        RuntimeException runtimeException = new RuntimeException("Database failure while cancelling order");

        Mockito.doNothing().when(orderValidationService).onDelete(orderId);
        Mockito.doThrow(runtimeException).when(orderService).delete(orderId);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.delete("/api/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isInternalServerError())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus(),
                "HTTP status not matched");

        // Verify response body (generic exception handler)
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Unexpected error");
        expectedErrorResponseDto
        .setDescription("An unexpected error has occurred on server. Please try after 5 to 10 minutes. "
                + "If the problem persists contact our support team at support@agilery.ai");

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for server side error during delete");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onDelete(orderId);
        Mockito.verify(orderService, Mockito.times(1)).delete(orderId);
    }

    /**
     * Tests the get all orders API for a valid pageable request.
     *
     * <p>
     * Verifies that when valid pagination parameters are provided, the API
     * retrieves the paginated list of orders successfully and returns HTTP status
     * {@code 200 (OK)}.
     * </p>
     */
    @Test
    void testSuccessForGetAll() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Pageable pageable = PageRequest.of(0, 10);

        OrderDto order1 = OrderTestHelper.createOrderDto();
        order1.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        OrderDto order2 = OrderTestHelper.createOrderDto();
        order2.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        List<OrderDto> orders = List.of(order1, order2);
        Page<OrderDto> orderPage = new PageImpl<>(orders, pageable, orders.size());

        Mockito.doNothing().when(orderValidationService).onGetAll(Mockito.any(Pageable.class));
        Mockito.when(orderService.getAll(Mockito.any(Pageable.class))).thenReturn(orderPage);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/orders").param("page", "0").param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.OK.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        String expected = objectMapper.writeValueAsString(orderPage);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Response body does not match for successful getAll orders");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onGetAll(pageable);
        Mockito.verify(orderService, Mockito.times(1)).getAll(pageable);
    }

    /**
     * Tests the get all orders API when an invalid sort field is provided.
     *
     * <p>
     * Verifies that when sorting is requested on a field that is not allowed, the
     * semantic validation fails and the API returns HTTP status
     * {@code 400 (Bad Request)}.
     * </p>
     */
    @Test
    void testSemanticValidationForGetAll() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        ValidationErrorDto validationErrorDto = new ValidationErrorDto();
        validationErrorDto.setObjectName("orderDto");
        validationErrorDto.setField("sort");
        validationErrorDto.setMessage(
                "Sorting by 'invalidField' is not allowed. Allowed fields are: '[createdDateTime, "
                        + "customerEmail, customerName, id, lastModifiedDateTime, orderDate, orderStatus, "
                        + "paymentMethod, totalAmount]'");

        List<ValidationErrorDto> validationErrors = List.of(validationErrorDto);

        ValidationException validationException = new ValidationException(validationErrors);

        Mockito.doThrow(validationException).when(orderValidationService).onGetAll(Mockito.any(Pageable.class));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/orders").param("page", "0").param("size", "10")
                        .param("sort", "invalidField,asc").contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus(), "HTTP status not matched");

        // Verify response headers
        Map<String, List<String>> actualHeaders = response.getHeaderNames().stream()
                .collect(Collectors.toMap(name -> name, response::getHeaders));

        Map<String, List<String>> expectedHeaders = TestHelper.getHeadersForTestingOfControllers();

        Assertions.assertEquals(expectedHeaders.toString(), actualHeaders.toString(),
                "Expected and actual headers does not match");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Request is not valid. Please check the validation errors for more details.");
        expectedErrorResponseDto.setValidationErrors(validationErrors);

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for invalid sort field semantic validation");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify no interaction with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onGetAll(Mockito.any(Pageable.class));
        Mockito.verify(orderService, Mockito.never()).getAll(Mockito.any(Pageable.class));
    }

    /**
     * Tests the get all orders API when an unexpected server error occurs.
     *
     * <p>
     * Verifies that when an unexpected exception is thrown while retrieving
     * paginated orders, the API returns HTTP status
     * {@code 500 (Internal Server Error)}.
     * </p>
     */
    @Test
    void testInternalServerErrorForGetAll() throws Exception {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Pageable pageable = PageRequest.of(0, 10);

        RuntimeException runtimeException = new RuntimeException("Database failure while fetching orders");

        Mockito.doNothing().when(orderValidationService).onGetAll(pageable);
        Mockito.when(orderService.getAll(Mockito.any(Pageable.class))).thenThrow(runtimeException);

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method that needs to be tested
        ////////////////////////////////////////////////////////////////////////
        MockHttpServletResponse response = mockMvc
                .perform(MockMvcRequestBuilders.get("/api/orders").param("page", "0").param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isInternalServerError())
                .andReturn().getResponse();

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result of the method to be tested
        ////////////////////////////////////////////////////////////////////////

        // Verify status
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus(),
                "HTTP status not matched");

        // Verify response body
        ErrorResponseDto expectedErrorResponseDto = new ErrorResponseDto();
        expectedErrorResponseDto.setMessage("Unexpected error");
        expectedErrorResponseDto
        .setDescription("An unexpected error has occurred on server. Please try after 5 to 10 minutes. "
                + "If the problem persists contact our support team at support@agilery.ai");

        String expected = objectMapper.writeValueAsString(expectedErrorResponseDto);
        String actual = response.getContentAsString();

        Assertions.assertEquals(expected.toString(), actual.toString(),
                "Error response does not match for server side error during getAll");

        ////////////////////////////////////////////////////////////////////////
        // 4. Verify interactions with dependent services
        ////////////////////////////////////////////////////////////////////////
        Mockito.verify(orderValidationService, Mockito.times(1)).onGetAll(pageable);
        Mockito.verify(orderService, Mockito.times(1)).getAll(pageable);
    }

}
