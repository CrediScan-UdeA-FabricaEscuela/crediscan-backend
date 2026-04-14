package co.udea.codefactory.creditscoring.scoring.domain.exception;

import co.udea.codefactory.creditscoring.shared.exception.DomainException;

public class ScoringVariableValidationException extends DomainException {

    public ScoringVariableValidationException(String message) {
        super(message);
    }

    @Override
    public String errorCode() {
        return "SCORING_VARIABLE_VALIDATION_FAILED";
    }
}
