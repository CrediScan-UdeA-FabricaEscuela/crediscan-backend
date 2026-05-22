package co.udea.codefactory.creditscoring.creditdecision.domain.port.in;

import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;

public interface GetCreditDecisionUseCase {

    Optional<CreditDecision> findByEvaluationId(UUID evaluationId);

    boolean existsByEvaluationId(UUID evaluationId);
}
