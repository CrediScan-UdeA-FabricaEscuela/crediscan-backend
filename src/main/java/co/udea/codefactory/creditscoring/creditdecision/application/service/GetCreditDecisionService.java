package co.udea.codefactory.creditscoring.creditdecision.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.in.GetCreditDecisionUseCase;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.CreditDecisionRepositoryPort;

@Service
public class GetCreditDecisionService implements GetCreditDecisionUseCase {

    private final CreditDecisionRepositoryPort repository;

    public GetCreditDecisionService(CreditDecisionRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreditDecision> findByEvaluationId(UUID evaluationId) {
        return repository.findByEvaluationId(evaluationId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEvaluationId(UUID evaluationId) {
        return repository.existsByEvaluationId(evaluationId);
    }
}
