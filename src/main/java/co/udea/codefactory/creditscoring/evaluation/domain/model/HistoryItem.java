package co.udea.codefactory.creditscoring.evaluation.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ítem del historial de evaluaciones de un solicitante.
 * {@code scoreDelta} es nulo para la evaluación más antigua del historial.
 */
public record HistoryItem(
        UUID evaluationId,
        OffsetDateTime evaluatedAt,
        BigDecimal totalScore,
        RiskLevel riskLevel,
        String modelName,
        int modelVersion,
        String evaluatedBy,
        boolean knockedOut,
        BigDecimal scoreDelta) {
}
