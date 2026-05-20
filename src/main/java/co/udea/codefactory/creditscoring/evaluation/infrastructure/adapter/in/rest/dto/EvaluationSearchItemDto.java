package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para un ítem del listado de búsqueda de evaluaciones.
 */
public record EvaluationSearchItemDto(
        UUID evaluationId,
        UUID applicantId,
        String applicantName,
        OffsetDateTime evaluatedAt,
        BigDecimal score,
        String riskLevel,
        String decisionStatus,
        String analista
) {}
