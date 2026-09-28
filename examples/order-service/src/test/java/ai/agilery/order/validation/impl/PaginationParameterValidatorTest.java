package ai.agilery.order.validation.impl;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ai.agilery.order.dto.ValidationErrorDto;

/**
 * Unit tests for {@link OrderPaginationParameterValidator}.
 *
 * <p>
 * Verifies that pagination sorting parameters are validated against the allowed
 * Order entity fields.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PaginationParameterValidatorTest {

    /**
     * Verifies that validation success when all requested sort fields are allowed
     * for orders.
     * <p>
     * In this case, no validation errors should be returned.
     * </p>
     */
    @Test
    void testForValidationSuccess() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Set<String> allowedFields = Set.of("id", "orderStatus", "createdDateTime");
        PaginationParameterValidator validator = new PaginationParameterValidator(allowedFields, "orderDto");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id", "orderStatus"));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(pageable);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertTrue(errors.isEmpty(), "Order: valid sort fields should produce no validation errors");
    }

    /**
     * Verifies that validation fails when a requested sort field is not allowed for
     * orders.
     * <p>
     * An invalid sort field must result in a validation error.
     * </p>
     */
    @Test
    void testForValidationFailure() {

        ////////////////////////////////////////////////////////////////////////
        // 1. Test data setup
        ////////////////////////////////////////////////////////////////////////
        Set<String> allowedFields = Set.of("id", "orderStatus", "createdDateTime");
        PaginationParameterValidator validator = new PaginationParameterValidator(allowedFields, "orderDto");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("totalAmount"));

        ////////////////////////////////////////////////////////////////////////
        // 2. Call method under test
        ////////////////////////////////////////////////////////////////////////
        List<ValidationErrorDto> errors = validator.validate(pageable);

        ////////////////////////////////////////////////////////////////////////
        // 3. Verify result
        ////////////////////////////////////////////////////////////////////////
        Assertions.assertEquals(1, errors.size(), "Expected one validation error for invalid sort field");
        ValidationErrorDto error = errors.get(0);
        Assertions.assertEquals("orderDto", error.getObjectName());
        Assertions.assertEquals("sort", error.getField());
        Assertions.assertEquals(
                "Sorting by 'totalAmount' is not allowed. Allowed fields are: '[createdDateTime, id, orderStatus]'",
                error.getMessage());
    }
}
