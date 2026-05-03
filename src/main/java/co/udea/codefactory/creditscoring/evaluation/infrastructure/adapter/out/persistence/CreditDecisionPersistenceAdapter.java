package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import co.udea.codefactory.creditscoring.evaluation.domain.model.DecisionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.evaluation.domain.model.CreditDecision;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.CreditDecisionRepositoryPort;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreditDecisionPersistenceAdapter implements CreditDecisionRepositoryPort {

    private final JpaCreditDecisionRepository repository;

    @Override
    public boolean existsByEvaluationId(UUID evaluationId) {
        return repository.existsByEvaluationId(evaluationId);
    }

    @Override
    public void save(CreditDecision decision) {

        System.out.println(">> SAVE DECISION ENTERED");

        CreditDecisionJpaEntity entity = new CreditDecisionJpaEntity();

        entity.setId(decision.getId()); // usa el id del dominio
        entity.setEvaluationId(decision.getEvaluationId());
        entity.setDecision(decision.getDecision().name());
        entity.setObservations(decision.getObservations());


        entity.setDecidedBy(decision.getDecidedBy());
        entity.setDecidedAt(decision.getDecidedAt());
        entity.setCreatedAt(decision.getCreatedAt());
        entity.setCreatedBy(decision.getCreatedBy());


        entity.setApprovedAmount(decision.getApprovedAmount());
        entity.setInterestRate(decision.getInterestRate());
        entity.setTermMonths(decision.getTermMonths());
        System.out.println(">> DECIDED BY: " + decision.getDecidedBy());
        System.out.println(">> DECIDED AT: " + decision.getDecidedAt());
        System.out.println(">> CREATED BY: " + decision.getCreatedBy());
        System.out.println(">> CREATED AT: " + decision.getCreatedAt());
        System.out.println(">> BEFORE SAVE");
        repository.save(entity);
        System.out.println(">> AFTER SAVE");
    }
}