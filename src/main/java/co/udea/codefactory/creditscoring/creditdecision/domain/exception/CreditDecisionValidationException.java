package co.udea.codefactory.creditscoring.creditdecision.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

/**
 * Excepción lanzada cuando las observaciones de la decisión no cumplen
 * con el mínimo de 20 caracteres requerido.
 *
 * <p>Implementa CA3: Se requiere campo de observaciones obligatorio con mínimo 20 caracteres.</p>
 */
public class CreditDecisionValidationException extends DomainException {

    public CreditDecisionValidationException(String message) {
        super(message);
    }

    @Override
    public int httpStatusCode() {
        return 400;
    }

    @Override
    public String errorCode() {
        return "CREDIT_DECISION_VALIDATION_FAILED";
    }
}
