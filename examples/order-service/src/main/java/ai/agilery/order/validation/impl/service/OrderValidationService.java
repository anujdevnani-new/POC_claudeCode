package ai.agilery.order.validation.impl.service;

import ai.agilery.order.dto.OrderDto;
import ai.agilery.order.dto.ValidationErrorDto;
import ai.agilery.order.validation.AbstractValidationService;
import ai.agilery.order.validation.impl.chain.OrderCreateValidationChain;
import ai.agilery.order.validation.impl.chain.OrderDeleteValidationChain;
import ai.agilery.order.validation.impl.chain.OrderGetAllValidationChain;
import ai.agilery.order.validation.impl.chain.OrderGetValidationChain;
import ai.agilery.order.validation.impl.chain.OrderPatchValidationChain;
import ai.agilery.order.validation.impl.chain.OrderUpdateValidationChain;
import ai.agilery.order.validation.util.CommonValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for performing semantic validation on {@link OrderDto} objects
 * and order IDs across various CRUD operations.
 * <p>
 * This service orchestrates different validation chains to ensure business rules
 * are met before processing requests. All methods run in a read-only transaction.
 * </p>
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class OrderValidationService implements AbstractValidationService<OrderDto, UUID> {

    /**
     * Validation chain for order creation operations.
     */
    private final OrderCreateValidationChain orderCreateValidationChain;

    /**
     * Validation chain for order update operations.
     */
    private final OrderUpdateValidationChain orderUpdateValidationChain;

    /**
     * Validation chain for order patch (partial update) operations.
     */
    private final OrderPatchValidationChain orderPatchValidationChain;

    /**
     * Validation chain for retrieving a single order by ID.
     */
    private final OrderGetValidationChain orderGetValidationChain;

    /**
     * Validation chain for retrieving all orders with pagination.
     */
    private final OrderGetAllValidationChain orderGetAllValidationChain;

    /**
     * Validation chain for order deletion (cancellation) operations.
     */
    private final OrderDeleteValidationChain orderDeleteValidationChain;

    /**
     * Utility for throwing common validation exceptions.
     */
    private final CommonValidationUtil commonValidationUtil;

    /**
     * Performs validation for creating a new order.
     * Ensures that the order has items and the total amount matches item subtotals.
     *
     * @param dto The {@link OrderDto} to validate.
     */
    @Override
    public void onCreate(OrderDto dto) {
        log.info("Starting onCreate validation for OrderDto: {}", dto.getCustomerEmail());
        List<ValidationErrorDto> errors = orderCreateValidationChain.validate(dto);
        commonValidationUtil.throwValidationException(errors);
        log.info("onCreate validation passed for OrderDto: {}", dto.getCustomerEmail());
    }

    /**
     * Performs validation for updating an existing order.
     * Ensures that the order exists and the total amount matches item subtotals.
     *
     * @param dto The {@link OrderDto} to validate.
     */
    @Override
    public void onUpdate(OrderDto dto) {
        log.info("Starting onUpdate validation for OrderDto ID: {}", dto.getId());
        List<ValidationErrorDto> errors = orderUpdateValidationChain.validate(dto);
        commonValidationUtil.throwValidationException(errors);
        log.info("onUpdate validation passed for OrderDto ID: {}", dto.getId());
    }

    /**
     * Performs validation for partially updating an existing order.
     * Ensures that the order exists and any status transition is valid.
     *
     * @param dto The {@link OrderDto} to validate.
     */
    @Override
    public void onPatch(OrderDto dto) {
        log.info("Starting onPatch validation for OrderDto ID: {}", dto.getId());
        List<ValidationErrorDto> errors = orderPatchValidationChain.validate(dto);
        commonValidationUtil.throwValidationException(errors);
        log.info("onPatch validation passed for OrderDto ID: {}", dto.getId());
    }

    /**
     * Performs validation for retrieving an order by its ID.
     * Ensures that the order with the given ID exists.
     *
     * @param id The ID of the order to retrieve.
     */
    @Override
    public void onGet(UUID id) {
        log.info("Starting onGet validation for Order ID: {}", id);
        List<ValidationErrorDto> errors = orderGetValidationChain.validate(id);
        commonValidationUtil.throwResourceNotFoundException(errors, String.format("Order with '%s' does not exist", id));
        log.info("onGet validation passed for Order ID: {}", id);
    }

    /**
     * Performs validation for retrieving all orders with pagination and sorting.
     * Ensures that sorting parameters are valid.
     *
     * @param pageable The {@link Pageable} object containing pagination and sorting information.
     */
    @Override
    public void onGetAll(Pageable pageable) {
        log.info("Starting onGetAll validation for Pageable: {}", pageable);
        List<ValidationErrorDto> errors = orderGetAllValidationChain.validate(pageable);
        commonValidationUtil.throwValidationException(errors);
        log.info("onGetAll validation passed for Pageable: {}", pageable);
    }

    /**
     * Performs validation for deleting (cancelling) an order by its ID.
     * Ensures that the order exists and its status allows for cancellation.
     *
     * @param id The ID of the order to delete.
     */
    public void onDelete(UUID id) {
        log.info("Starting onDelete validation for Order ID: {}", id);
        List<ValidationErrorDto> errors = orderDeleteValidationChain.validate(id);
        commonValidationUtil.throwValidationException(errors);
        log.info("onDelete validation passed for Order ID: {}", id);
    }
}
