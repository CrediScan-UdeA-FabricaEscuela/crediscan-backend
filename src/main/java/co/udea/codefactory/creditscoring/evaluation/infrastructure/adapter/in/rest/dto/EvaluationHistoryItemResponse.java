package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para un ítem del historial de evaluaciones de un solicitante.
 * {@code scoreDelta} puede ser null para la evaluación más antigua.
 */
public record EvaluationHistoryItemResponse(
        UUID evaluationId,
        OffsetDateTime evaluatedAt,
        BigDecimal totalScore,
        String riskLevel,
        String modelName,
        int modelVersion,
        String evaluatedBy,
        boolean knockedOut,
        BigDecimal scoreDelta) {
}
