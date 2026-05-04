package co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.out.persistence;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;

/**
 * Mapper entre el modelo de dominio de decisión crediticia y la entidad JPA.
 */
@Component
public class CreditDecisionPersistenceMapper {

    /** Convierte el modelo de dominio a entidad JPA. */
    public CreditDecisionJpaEntity toJpaEntity(CreditDecision creditDecision) {
        return new CreditDecisionJpaEntity(
                creditDecision.id(),
                creditDecision.evaluationId(),
                creditDecision.decision(),
                creditDecision.observations(),
                creditDecision.analystId(),
                creditDecision.decidedAt(),
                creditDecision.createdAt(),
                creditDecision.createdBy(),
                creditDecision.supervisorId(),
                creditDecision.resolutionDeadlineAt()
        );
    }

    /** Convierte la entidad JPA al modelo de dominio. */
    public CreditDecision toDomain(CreditDecisionJpaEntity entity) {
        return CreditDecision.rehydrate(
                entity.getId(),
                entity.getEvaluationId(),
                entity.getDecision(),
                entity.getObservations(),
                entity.getDecidedBy(),
                entity.getDecidedAt(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getSupervisorId(),
                entity.getResolutionDeadlineAt()
        );
    }
}
