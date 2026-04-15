package co.udea.codefactory.creditscoring.evaluation.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

/**
 * Lanzada cuando se intenta evaluar a un solicitante que ya fue evaluado
 * dentro del período de cooldown configurado.
 */
public class EvaluationCooldownException extends DomainException {

    public EvaluationCooldownException(String message) {
        super(message);
    }

    @Override
    public int httpStatusCode() {
        return 409;
    }

    @Override
    public String errorCode() {
        return "EVALUATION_COOLDOWN_ACTIVE";
    }
}
