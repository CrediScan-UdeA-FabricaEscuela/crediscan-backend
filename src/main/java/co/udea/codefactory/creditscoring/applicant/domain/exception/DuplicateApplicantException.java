package co.udea.codefactory.creditscoring.applicant.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

public class DuplicateApplicantException extends DomainException {

    public DuplicateApplicantException(String message) {
        super(message);
    }

    @Override
    public int httpStatusCode() {
        return 409;
    }

    @Override
    public String errorCode() {
        return "DUPLICATE_RESOURCE";
    }
}