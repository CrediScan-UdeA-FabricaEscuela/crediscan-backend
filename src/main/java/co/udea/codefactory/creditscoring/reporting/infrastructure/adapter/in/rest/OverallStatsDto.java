package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.in.rest;

import java.math.BigDecimal;

import co.udea.codefactory.creditscoring.reporting.domain.model.OverallStats;

/**
 * DTO para las estadísticas globales del reporte.
 */
public record OverallStatsDto(
        BigDecimal scorePromedio,
        BigDecimal desviacionEstandar,
        long totalEvaluaciones
) {
    public static OverallStatsDto from(OverallStats o) {
        return new OverallStatsDto(o.averageScore(), o.stdDev(), o.totalEvaluations());
    }
}
