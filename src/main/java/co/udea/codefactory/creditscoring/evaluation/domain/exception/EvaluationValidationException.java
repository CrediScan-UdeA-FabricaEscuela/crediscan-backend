package co.udea.codefactory.creditscoring.evaluation.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

/**
 * Lanzada cuando una operación de evaluación viola una regla de negocio de validación,
 * por ejemplo: rango de fechas invertido o evaluaciones de solicitantes distintos en comparar.
 */
public class EvaluationValidationException extends DomainException {

    public EvaluationValidationException(String message) {
        super(message);
    }

    @Override
    public int httpStatusCode() {
        return 400;
    }

    @Override
    public String errorCode() {
        return "EVALUATION_VALIDATION";
    }
}
