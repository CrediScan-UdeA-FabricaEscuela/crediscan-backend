package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import co.udea.codefactory.creditscoring.evaluation.domain.model.CreditDecision;

import java.util.UUID;

public interface CreditDecisionRepositoryPort {

    boolean existsByEvaluationId(UUID evaluationId);

    void save(CreditDecision decision);
}