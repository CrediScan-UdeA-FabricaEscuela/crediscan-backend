package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import co.udea.codefactory.creditscoring.evaluation.application.dto.RegisterDecisionRequest;

import java.util.UUID;

public interface RegisterDecisionUseCase {

    void register(UUID evaluationId, RegisterDecisionRequest request);
}