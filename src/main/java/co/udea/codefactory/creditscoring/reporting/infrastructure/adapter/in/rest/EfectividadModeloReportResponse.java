package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.in.rest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.EfectividadModeloReporte;

/**
 * DTO de respuesta para el endpoint de efectividad del modelo (HU-016).
 */
public record EfectividadModeloReportResponse(
        List<CeldaMatrizDto> matriz,
        IndicadoresDto indicadores,
        List<CasoOverrideDto> overrides,
        boolean hasData,
        String mensaje,
        OffsetDateTime desde,
        OffsetDateTime hasta,
        String analistaId
) {
    /** Convierte el reporte de dominio a DTO de respuesta. */
    public static EfectividadModeloReportResponse from(EfectividadModeloReporte r) {
        String msg = r.hasData() ? null : "No hay evaluaciones con decisión en el rango especificado";

        List<CeldaMatrizDto> matrizDto = r.matriz().celdas().stream()
                .map(c -> new CeldaMatrizDto(
                        c.riskLevel().name(),
                        c.decision().name(),
                        c.count()))
                .toList();

        IndicadoresDto indicadoresDto = r.indicadores() == null ? null
                : new IndicadoresDto(
                        r.indicadores().tasaConcordanciaGlobal(),
                        r.indicadores().tasaOverrideAprobacion(),
                        r.indicadores().tasaOverrideRechazo(),
                        r.indicadores().totalCasos());

        List<CasoOverrideDto> overridesDto = r.overrides().stream()
                .map(o -> new CasoOverrideDto(
                        o.riskLevel().name(),
                        o.decision().name(),
                        o.count()))
                .toList();

        return new EfectividadModeloReportResponse(
                matrizDto, indicadoresDto, overridesDto,
                r.hasData(), msg, r.desde(), r.hasta(), r.analistaId());
    }

    /**
     * Celda de la matriz de confusión en la respuesta JSON.
     */
    public record CeldaMatrizDto(String riskLevel, String decision, long count) {}

    /**
     * Indicadores de efectividad en la respuesta JSON.
     */
    public record IndicadoresDto(
            BigDecimal concordanceRate,
            BigDecimal overrideApprovalRate,
            BigDecimal overrideRejectionRate,
            long totalCasos) {}

    /**
     * Caso de override en la respuesta JSON.
     */
    public record CasoOverrideDto(String riskLevel, String decision, long count) {}
}
