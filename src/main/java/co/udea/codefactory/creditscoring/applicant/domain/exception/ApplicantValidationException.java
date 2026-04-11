package co.udea.codefactory.creditscoring.applicant.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

public class ApplicantValidationException extends DomainException {

    public ApplicantValidationException(String message) {
        super(message);
    }

    @Override
    public String errorCode() {
        return "VALIDATION_FAILED";
    }
}