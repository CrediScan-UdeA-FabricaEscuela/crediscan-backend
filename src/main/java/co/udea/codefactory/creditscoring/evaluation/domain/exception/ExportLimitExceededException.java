package co.udea.codefactory.creditscoring.evaluation.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

/**
 * Lanzada cuando se intenta exportar a PDF más filas de las permitidas (máximo 1000).
 * Retorna HTTP 422 Unprocessable Entity: la solicitud es semánticamente válida
 * pero el estado del recurso impide procesarla.
 */
public class ExportLimitExceededException extends DomainException {

    public ExportLimitExceededException(long actual, long limit) {
        super("El export excede el límite (" + actual + " > " + limit + ")");
    }

    @Override
    public int httpStatusCode() {
        return 422;
    }

    @Override
    public String errorCode() {
        return "EXPORT_LIMIT_EXCEEDED";
    }
}
