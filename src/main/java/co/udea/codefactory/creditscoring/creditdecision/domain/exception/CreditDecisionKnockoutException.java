package co.udea.codefactory.creditscoring.creditdecision.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

/**
 * Excepción lanzada cuando se intenta registrar una decisión diferente a REJECTED
 * para una evaluación que fue rechazada por knock-out.
 *
 * <p>Implementa RN1/CA4: Si la evaluación fue rechazada por knock-out,
 * la única decisión posible es REJECTED.</p>
 */
public class CreditDecisionKnockoutException extends DomainException {

    public CreditDecisionKnockoutException(String message) {
        super(message);
    }

    @Override
    public int httpStatusCode() {
        return 400;
    }

    @Override
    public String errorCode() {
        return "CREDIT_DECISION_KNOCKOUT_RESTRICTION";
    }
}
