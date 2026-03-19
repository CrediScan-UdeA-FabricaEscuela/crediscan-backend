package co.udea.codefactory.creditscoring.applicant.domain.exception;

public class DuplicateApplicantException extends RuntimeException {

    public DuplicateApplicantException(String message) {
        super(message);
    }
}