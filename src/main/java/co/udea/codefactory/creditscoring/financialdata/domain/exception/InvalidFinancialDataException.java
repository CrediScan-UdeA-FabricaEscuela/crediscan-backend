package co.udea.codefactory.creditscoring.financialdata.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

public class InvalidFinancialDataException extends DomainException {

    public InvalidFinancialDataException(String message) {
        super(message);
    }

    @Override
    public String errorCode() {
        return "VALIDATION_FAILED";
    }
}
