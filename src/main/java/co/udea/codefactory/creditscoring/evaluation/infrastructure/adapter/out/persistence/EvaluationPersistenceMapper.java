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
        EvaluationJpaEntity entity = EvaluationJpaEntity.builder()
                .id(evaluation.id())
                .applicantId(evaluation.applicantId())
                .modelId(evaluation.modelId())
                .financialDataId(evaluation.financialDataId())
                .totalScore(evaluation.totalScore())
                .riskLevel(evaluation.riskLevel().name())
                .knockedOut(evaluation.knockedOut())
                .knockoutReasons(evaluation.knockoutReasons())
                .evaluatedAt(evaluation.evaluatedAt())
                .evaluatedBy(evaluation.evaluatedBy())
                .createdAt(evaluation.createdAt())
                .createdBy(evaluation.createdBy())
                .build();

        // Mapear detalles de variables manteniendo relación bidireccional
        for (EvaluationDetail detail : evaluation.details()) {
            EvaluationDetailJpaEntity detailEntity = EvaluationDetailJpaEntity.builder()
                    .id(detail.id())
                    .variableId(detail.variableId())
                    .variableName(detail.variableName())
                    .rawValue(detail.rawValue())
                    .score(detail.score())
                    .weight(detail.weight())
                    .weightedScore(detail.weightedScore())
                    .createdAt(detail.createdAt())
                    .build();
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
