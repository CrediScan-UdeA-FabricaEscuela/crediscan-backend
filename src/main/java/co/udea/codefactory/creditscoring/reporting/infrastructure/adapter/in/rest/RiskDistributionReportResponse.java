package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.in.rest;

import java.time.OffsetDateTime;
import java.util.List;

import co.udea.codefactory.creditscoring.reporting.domain.model.RiskDistributionReport;

/**
 * DTO de respuesta para el endpoint de distribución de riesgo.
 */
public record RiskDistributionReportResponse(
        List<RiskLevelSummaryDto> tabla,
        List<HistogramBinDto> histograma,
        OverallStatsDto overall,
        boolean hasData,
        String mensaje,
        OffsetDateTime fechaDesde,
        OffsetDateTime fechaHasta,
        String tipoEmpleo
) {
    public static RiskDistributionReportResponse from(RiskDistributionReport r) {
        String msg = r.hasData() ? null : "No hay evaluaciones en el rango especificado";
        return new RiskDistributionReportResponse(
                r.tabla().stream().map(RiskLevelSummaryDto::from).toList(),
                r.histograma().stream().map(HistogramBinDto::from).toList(),
                OverallStatsDto.from(r.overall()),
                r.hasData(),
                msg,
                r.fechaDesde(),
                r.fechaHasta(),
                r.tipoEmpleo());
    }
}
