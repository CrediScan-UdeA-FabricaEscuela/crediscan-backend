package co.udea.codefactory.creditscoring.evaluation.application.dto;

import co.udea.codefactory.creditscoring.evaluation.domain.model.DecisionType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDecisionRequest {

    private DecisionType decision;

    private String observations;

    private String analyst;
}