package ai.agilery.example.validation;

import java.util.ArrayList;
import java.util.List;

import ai.agilery.example.dto.ValidationErrorDto;

/**
 * Abstract base class for constructing and executing a chain of validation
 * handlers for a given DTO type.
 * <p>
 * This class follows the <strong>Chain of Responsibility</strong> design
 * pattern, where multiple {@link ValidationHandler} instances are executed
 * sequentially to validate an input DTO. Each handler contributes zero or more
 * validation errors, and the accumulated list of {@link ValidationErrorDto}
 * objects is returned.
 * </p>
 *
 * <p>
 * Implementations must override {@link #initializeChain()} to configure the
 * validation handlers in the desired order.
 * </p>
 *
 * @param <T> the type of DTO that this validation chain will validate
 */
public abstract class AbstractValidationChain<T> {

    /**
     * The ordered list of validation handlers that form the validation chain.
     * <p>
     * Each handler is executed in sequence when {@link #validate(Object)} is
     * called. Handlers can add one or more {@link ValidationErrorDto} objects based
     * on the validation logic they perform.
     * </p>
     */
    private final List<ValidationHandler<T>> chain = new ArrayList<>();

    /**
     * Implementations should populate the handler in the order in which handlers
     * should execute.
     *
     * @param handler The handler to be added.
     */
    protected void add(ValidationHandler<T> handler) {
        this.chain.add(handler);
    }

    /**
     * Executes all validation handlers in the chain against the given DTO.
     *
     * @param dto the DTO to validate; must not be {@code null}
     * @return the list of validation errors accumulated from all handlers; never
     *         {@code null}
     */
    public List<ValidationErrorDto> validate(T dto) {
        List<ValidationErrorDto> errors = new ArrayList<>();
        for (ValidationHandler<T> handler : chain) {
            errors.addAll(handler.validate(dto));
        }
        return errors;
    }

    /**
     * Returns the list of validation handlers that make up this validation chain.
     *
     * @return the list of handlers in execution order
     */
    public List<ValidationHandler<T>> getChain() {
        return chain;
    }
}
