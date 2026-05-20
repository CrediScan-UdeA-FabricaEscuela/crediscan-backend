package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para un ítem de la lista de clasificación por nivel de riesgo.
 */
public record ClassificationItemResponse(
        UUID evaluationId,
        UUID applicantId,
        String applicantName,
        BigDecimal totalScore,
        String riskLevel,
        OffsetDateTime evaluatedAt,
        String evaluatedBy) {
}
