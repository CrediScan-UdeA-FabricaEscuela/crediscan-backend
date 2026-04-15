package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetail;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationKnockout;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Mapper entre el modelo de dominio de evaluación y las entidades JPA.
 * Mantiene la separación entre la capa de persistencia y el dominio.
 */
@Component
public class EvaluationPersistenceMapper {

    /** Convierte el agregado de dominio a entidad JPA con sus colecciones. */
    public EvaluationJpaEntity toJpaEntity(Evaluation evaluation) {
        EvaluationJpaEntity entity = new EvaluationJpaEntity(
                evaluation.id(),
                evaluation.applicantId(),
                evaluation.modelId(),
                evaluation.financialDataId(),
                evaluation.totalScore(),
                evaluation.riskLevel().name(),
                evaluation.knockedOut(),
                evaluation.knockoutReasons(),
                evaluation.evaluatedAt(),
                evaluation.evaluatedBy(),
                evaluation.createdAt(),
                evaluation.createdBy()
        );

        // Mapear detalles de variables manteniendo relación bidireccional
        for (EvaluationDetail detail : evaluation.details()) {
            EvaluationDetailJpaEntity detailEntity = new EvaluationDetailJpaEntity(
                    detail.id(),
                    detail.variableId(),
                    detail.variableName(),
                    detail.rawValue(),
                    detail.score(),
                    detail.weight(),
                    detail.weightedScore(),
                    detail.createdAt()
            );
            entity.addDetail(detailEntity);
        }

        // Mapear resultados de reglas knockout manteniendo relación bidireccional
        for (EvaluationKnockout knockout : evaluation.knockouts()) {
            EvaluationKnockoutJpaEntity koEntity = new EvaluationKnockoutJpaEntity(
                    knockout.id(),
                    knockout.ruleId(),
                    knockout.ruleName(),
                    knockout.fieldValue(),
                    knockout.triggered(),
                    knockout.createdAt()
            );
            entity.addKnockout(koEntity);
        }

        return entity;
    }

    /** Reconstruye el agregado de dominio desde la entidad JPA. */
    public Evaluation toDomain(EvaluationJpaEntity entity) {
        List<EvaluationDetail> details = entity.getDetails().stream()
                .map(d -> EvaluationDetail.rehydrate(
                        d.getId(), d.getVariableId(), d.getVariableName(),
                        d.getRawValue(), d.getScore(), d.getWeight(), d.getWeightedScore(),
                        d.getCreatedAt()))
                .toList();

        List<EvaluationKnockout> knockouts = entity.getKnockouts().stream()
                .map(k -> EvaluationKnockout.rehydrate(
                        k.getId(), k.getRuleId(), k.getRuleName(),
                        k.getFieldValue(), k.isTriggered(), k.getCreatedAt()))
                .toList();

        return Evaluation.rehydrate(
                entity.getId(),
                entity.getApplicantId(),
                entity.getModelId(),
                entity.getFinancialDataId(),
                entity.getTotalScore(),
                RiskLevel.valueOf(entity.getRiskLevel()),
                entity.isKnockedOut(),
                entity.getKnockoutReasons(),
                entity.getEvaluatedAt(),
                entity.getEvaluatedBy(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                details,
                knockouts
        );
    }
}
