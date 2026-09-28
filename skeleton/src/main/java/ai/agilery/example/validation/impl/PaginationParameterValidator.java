package ai.agilery.example.validation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ai.agilery.example.dto.ValidationErrorDto;
import ai.agilery.example.validation.ValidationHandler;

/**
 * Validates pagination and sorting parameters supplied via Spring's
 * {@link Pageable} interface.
 * <p>
 * This validator ensures that only predefined fields are allowed to be used as
 * sorting parameters. If a client requests sorting on a field that is not in
 * the allowed whitelist, a {@link ValidationErrorDto} entry is returned
 * describing the violation.
 * </p>
 *
 * <p>
 * Typical usage is in request-level validation where pagination and sorting
 * parameters must be restricted to prevent querying unintended fields.
 * </p>
 */
public class PaginationParameterValidator implements ValidationHandler<Pageable> {

    /**
     * Set of field names that are allowed to be used as sort parameters. Any
     * sorting request outside of this set will be reported as a validation error.
     */
    private final Set<String> allowedSortFields;

    /**
     * Name of the object being validated. Used when constructing
     * {@link ValidationErrorDto} entries to identify the source of the validation
     * error.
     */
    private final String objectName;

    /**
     * Creates a new {@code PaginationParameterValidator}.
     *
     * @param allowedSortFields the whitelist of allowed field names for sorting
     * @param objectName        the logical name of the object being validated
     *                          (e.g., DTO name)
     */
    public PaginationParameterValidator(Set<String> allowedSortFields, String objectName) {
        this.allowedSortFields = allowedSortFields;
        this.objectName = objectName;
    }

    /**
     * Validates the sorting parameters inside the provided {@link Pageable}
     * instance.
     * <p>
     * For each {@link Sort.Order}, this method checks whether the requested sort
     * property is part of the allowed fields. If not, a {@link ValidationErrorDto}
     * describing the issue is added to the returned list.
     * </p>
     *
     * @param pageable the pageable object containing page, size, and sort
     *                 information
     * @return a list of validation errors; empty if all sorting parameters are
     *         valid
     */
    @Override
    public List<ValidationErrorDto> validate(Pageable pageable) {
        List<ValidationErrorDto> validationErrorDtos = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSortFields.contains(order.getProperty().toString())) {
                ValidationErrorDto validationErrorDto = new ValidationErrorDto();
                validationErrorDto.setObjectName(objectName);
                validationErrorDto.setField("sort");
                validationErrorDto.setMessage(String.format("Sorting by '%s' is not allowed. Allowed fields are: '%s'",
                        order.getProperty(), new TreeSet<String>(allowedSortFields)));
                validationErrorDtos.add(validationErrorDto);
            }
        }

        return validationErrorDtos;
    }

}
