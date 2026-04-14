package co.udea.codefactory.creditscoring.scoringmodel.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

public class ScoringModelValidationException extends DomainException {

    public ScoringModelValidationException(String message) {
        super(message);
    }

    @Override
    public String errorCode() {
        return "SCORING_MODEL_VALIDATION_FAILED";
    }
}
