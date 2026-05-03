package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaCreditDecisionRepository
        extends JpaRepository<CreditDecisionJpaEntity, UUID> {

    boolean existsByEvaluationId(UUID evaluationId);
}