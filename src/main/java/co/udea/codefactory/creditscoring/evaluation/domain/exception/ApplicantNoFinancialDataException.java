package co.udea.codefactory.creditscoring.evaluation.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

/**
 * Lanzada cuando un solicitante no tiene datos financieros registrados
 * y por lo tanto no puede ser evaluado.
 */
public class ApplicantNoFinancialDataException extends DomainException {

    public ApplicantNoFinancialDataException(String message) {
        super(message);
    }

    @Override
    public int httpStatusCode() {
        return 422;
    }

    @Override
    public String errorCode() {
        return "APPLICANT_NO_FINANCIAL_DATA";
    }
}
