package ai.agilery.order.validation;

import org.springframework.data.domain.Pageable;

/**
 * Defines lifecycle validation hooks for various CRUD operations on a DTO.
 * <p>
 * This interface provides default no-op methods that can be overridden by
 * implementing services to perform custom validation logic before executing
 * create, update, patch, or retrieval operations.
 * </p>
 *
 * <p>
 * The goal of this abstraction is to allow services to plug in validation
 * without repeating boilerplate logic and to keep controller or service layers
 * clean and consistent.
 * </p>
 *
 * @param <D> the type of the Data Transfer Object (DTO) being validated
 * @param <I> the type of the resource identifier (e.g., Long, UUID)
 */
public interface AbstractValidationService<D, I> {

    /**
     * Validation hook invoked before creating a new resource.
     *
     * @param dto the DTO representing the resource to be created
     */
    default void onCreate(D dto) {
        // Default no-op
    }

    /**
     * Validation hook invoked before updating an existing resource.
     *
     * <p>
     * This method is typically used to ensure that all required fields are present
     * and that the update request conforms to business rules.
     * </p>
     *
     * @param dto the DTO representing the updated state of the resource
     */
    default void onUpdate(D dto) {
        // Default no-op
    }

    /**
     * Validation hook invoked before applying a partial update (PATCH).
     *
     * <p>
     * Unlike update operations, patch operations may provide only a subset of
     * fields, so validation rules may differ.
     * </p>
     *
     * @param dto the DTO containing partial updates
     */
    default void onPatch(D dto) {
        // Default no-op
    }

    /**
     * Validation hook invoked before retrieving a resource by its identifier.
     *
     * @param id the identifier of the resource to retrieve
     */
    default void onGet(I id) {
        // Default no-op
    }

    /**
     * Validation hook invoked before retrieving a pageable list of resources.
     *
     * <p>
     * This method is commonly used to validate pagination parameters, sorting
     * fields, and filtering constraints.
     * </p>
     *
     * @param pageable pagination and sorting information
     */
    default void onGetAll(Pageable pageable) {
        // Default no-op
    }
}
