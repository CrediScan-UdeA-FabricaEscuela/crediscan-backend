package co.udea.codefactory.creditscoring.applicant.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

/**
 * Thrown when a client attempts to modify an immutable field (e.g., identification).
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler with errorCode IMMUTABLE_FIELD.
 */
public class ImmutableFieldException extends DomainException {

    private final String fieldName;

    public ImmutableFieldException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String errorCode() {
        return "IMMUTABLE_FIELD";
    }
}
