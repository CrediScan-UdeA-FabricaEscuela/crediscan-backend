package co.udea.codefactory.creditscoring.evaluation.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Detalle del puntaje obtenido por una variable de scoring dentro de una evaluación.
 * Registro inmutable con factory methods para creación y rehidratación.
 */
public record EvaluationDetail(
        UUID id,
        UUID variableId,
        String variableName,
        String rawValue,
        BigDecimal score,
        BigDecimal weight,
        BigDecimal weightedScore,
        OffsetDateTime createdAt
) {

    public EvaluationDetail {
        if (id == null) throw new IllegalArgumentException("El id del detalle es obligatorio");
        if (variableId == null) throw new IllegalArgumentException("El variableId es obligatorio");
        if (variableName == null || variableName.isBlank())
            throw new IllegalArgumentException("El nombre de la variable es obligatorio");
    }

    /** Crea un nuevo detalle de evaluación con id y timestamp generados. */
    public static EvaluationDetail crear(UUID variableId, String variableName, String rawValue,
            BigDecimal score, BigDecimal weight, BigDecimal weightedScore) {
        return new EvaluationDetail(UUID.randomUUID(), variableId, variableName,
                rawValue, score, weight, weightedScore, OffsetDateTime.now());
    }

    /** Rehidrata un detalle de evaluación desde persistencia con todos sus campos. */
    public static EvaluationDetail rehydrate(UUID id, UUID variableId, String variableName,
            String rawValue, BigDecimal score, BigDecimal weight, BigDecimal weightedScore,
            OffsetDateTime createdAt) {
        return new EvaluationDetail(id, variableId, variableName,
                rawValue, score, weight, weightedScore, createdAt);
    }
}
