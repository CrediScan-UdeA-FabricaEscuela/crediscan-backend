package co.udea.codefactory.creditscoring.creditdecision.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

/**
 * Excepción lanzada cuando se intenta registrar una decisión para una evaluación
 * que ya tiene una decisión asociada.
 *
 * <p>Implementa CA1: Solo se puede registrar una decisión por evaluación.</p>
 */
public class CreditDecisionAlreadyExistsException extends DomainException {

    public CreditDecisionAlreadyExistsException(String message) {
        super(message);
    }

    @Override
    public int httpStatusCode() {
        return 409;
    }

    @Override
    public String errorCode() {
        return "CREDIT_DECISION_ALREADY_EXISTS";
    }
}
