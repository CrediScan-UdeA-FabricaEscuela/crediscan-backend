package co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para credit_decision.
 */
public interface JpaCreditDecisionRepository extends JpaRepository<CreditDecisionJpaEntity, UUID> {

    /**
     * Verifica si existe una decisión para la evaluación dada.
     */
    boolean existsByEvaluationId(UUID evaluationId);

    /**
     * Busca una decisión por evaluation_id.
     */
    Optional<CreditDecisionJpaEntity> findByEvaluationId(UUID evaluationId);
}
