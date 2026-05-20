package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta con el detalle completo de una evaluación crediticia.
 * Incluye datos del modelo, solicitante, desglose de variables y knockouts.
 */
public record EvaluationDetailResponse(
        UUID id,
        UUID applicantId,
        String applicantName,
        UUID modelId,
        String modelName,
        int modelVersion,
        UUID financialDataId,
        BigDecimal totalScore,
        String riskLevel,
        boolean knockedOut,
        String knockoutReasons,
        OffsetDateTime evaluatedAt,
        String evaluatedBy,
        List<DetailItemDto> details,
        List<KnockoutItemDto> knockouts) {

    /** Desglose del puntaje parcial por variable de scoring. */
    public record DetailItemDto(
            UUID id,
            UUID variableId,
            String variableName,
            String rawValue,
            BigDecimal score,
            BigDecimal weight,
            BigDecimal weightedScore) {
    }

    /** Resultado de la evaluación de una regla knockout. */
    public record KnockoutItemDto(
            UUID id,
            UUID ruleId,
            String ruleName,
            String fieldValue,
            boolean triggered) {
    }
}
