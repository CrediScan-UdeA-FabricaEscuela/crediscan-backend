package co.udea.codefactory.creditscoring.applicant.domain.exception;

/**
 * Thrown when a client attempts to modify an immutable field (e.g., identification).
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler with errorCode IMMUTABLE_FIELD.
 */
public class ImmutableFieldException extends RuntimeException {

    private final String fieldName;

    public ImmutableFieldException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
