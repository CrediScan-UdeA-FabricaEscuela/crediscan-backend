package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta que representa el resultado completo de una evaluación crediticia.
 * Incluye el desglose de variables y los resultados de reglas knockout.
 */
public record EvaluationResponse(
        UUID id,
        UUID applicantId,
        UUID modelId,
        UUID financialDataId,
        BigDecimal totalScore,
        String riskLevel,
        boolean knockedOut,
        String knockoutReasons,
        OffsetDateTime evaluatedAt,
        String evaluatedBy,
        List<DetailDto> details,
        List<KnockoutDto> knockouts
) {

    /** Desglose del puntaje parcial obtenido por cada variable de scoring. */
    public record DetailDto(
            UUID id,
            UUID variableId,
            String variableName,
            String rawValue,
            BigDecimal score,
            BigDecimal weight,
            BigDecimal weightedScore
    ) {}

    /** Resultado de la evaluación de cada regla knockout. */
    public record KnockoutDto(
            UUID id,
            UUID ruleId,
            String ruleName,
            String fieldValue,
            boolean triggered
    ) {}
}
