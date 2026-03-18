package co.udea.codefactory.creditscoring.shared.exception;

/**
 * Thrown when a requested resource cannot be found.
 *
 * <p>Handled globally by {@link GlobalExceptionHandler} to return a 404 response
 * with a standardized RFC 7807 Problem Detail body.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
