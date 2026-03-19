package co.udea.codefactory.creditscoring.applicant.domain.exception;

public class ApplicantValidationException extends RuntimeException {

    public ApplicantValidationException(String message) {
        super(message);
    }
}