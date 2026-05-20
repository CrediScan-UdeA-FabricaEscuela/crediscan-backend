package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.in.rest;

import java.math.BigDecimal;

import co.udea.codefactory.creditscoring.reporting.domain.model.RiskLevelSummary;

/**
 * DTO para cada fila de la tabla resumen por nivel de riesgo.
 */
public record RiskLevelSummaryDto(
        String nivel,
        long cantidad,
        BigDecimal porcentaje,
        BigDecimal scorePromedio
) {
    public static RiskLevelSummaryDto from(RiskLevelSummary s) {
        return new RiskLevelSummaryDto(
                s.level().name(),
                s.count(),
                s.percentage(),
                s.averageScore());
    }
}
