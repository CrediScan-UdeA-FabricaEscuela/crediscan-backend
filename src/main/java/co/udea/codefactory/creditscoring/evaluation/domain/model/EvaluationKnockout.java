package co.udea.codefactory.creditscoring.evaluation.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resultado de la evaluación de una regla knockout para un solicitante.
 * Registro inmutable que persiste la trazabilidad de cada regla evaluada.
 */
public record EvaluationKnockout(
        UUID id,
        UUID ruleId,
        String ruleName,
        String fieldValue,
        boolean triggered,
        OffsetDateTime createdAt
) {

    public EvaluationKnockout {
        if (id == null) throw new IllegalArgumentException("El id del knockout es obligatorio");
        if (ruleId == null) throw new IllegalArgumentException("El ruleId es obligatorio");
        if (ruleName == null || ruleName.isBlank())
            throw new IllegalArgumentException("El nombre de la regla es obligatorio");
        if (fieldValue == null) throw new IllegalArgumentException("El fieldValue es obligatorio");
    }

    /** Crea un nuevo registro de evaluación de knockout con id y timestamp generados. */
    public static EvaluationKnockout crear(UUID ruleId, String ruleName,
            String fieldValue, boolean triggered) {
        return new EvaluationKnockout(UUID.randomUUID(), ruleId, ruleName,
                fieldValue, triggered, OffsetDateTime.now());
    }

    /** Rehidrata un registro de knockout desde persistencia. */
    public static EvaluationKnockout rehydrate(UUID id, UUID ruleId, String ruleName,
            String fieldValue, boolean triggered, OffsetDateTime createdAt) {
        return new EvaluationKnockout(id, ruleId, ruleName, fieldValue, triggered, createdAt);
    }
}
