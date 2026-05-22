package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.in.rest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;

/**
 * DTO de respuesta para el endpoint de actividad de analistas (HU-017).
 */
public record ActividadAnalistasReportResponse(
        List<ActividadAnalistaDto> analistas,
        EstadisticasEquipoDto estadisticasEquipo,
        boolean hasData,
        String mensaje,
        OffsetDateTime desde,
        OffsetDateTime hasta
) {
    /** Convierte el reporte de dominio a DTO de respuesta. */
    public static ActividadAnalistasReportResponse from(ActividadAnalistasReporte r) {
        String msg = r.hasData() ? null : "No hay evaluaciones con decisión en el rango especificado";

        List<ActividadAnalistaDto> analistasDto = r.analistas().stream()
                .map(a -> new ActividadAnalistaDto(
                        a.evaluatedBy(),
                        a.nombre(),
                        a.totalEvaluaciones(),
                        new DistribucionDto(
                                a.distribucion().aprobadas(),
                                a.distribucion().rechazadas(),
                                a.distribucion().revisionManual(),
                                a.distribucion().escaladas(),
                                a.distribucion().pctAprobacion(),
                                a.distribucion().pctRechazo(),
                                a.distribucion().pctManual(),
                                a.distribucion().pctEscalado()),
                        a.tiempoMedioHorasHabiles(),
                        a.isOutlier()))
                .toList();

        EstadisticasEquipoDto equipoDto = new EstadisticasEquipoDto(
                r.estadisticasEquipo().totalEvaluacionesEquipo(),
                r.estadisticasEquipo().mediaEquipoHorasHabiles(),
                r.estadisticasEquipo().stddevHorasHabiles(),
                r.estadisticasEquipo().numAnalistas(),
                r.estadisticasEquipo().outlierDetectionSkipped(),
                r.estadisticasEquipo().tasaAprobacionEquipo());

        return new ActividadAnalistasReportResponse(
                analistasDto, equipoDto, r.hasData(), msg, r.desde(), r.hasta());
    }

    /** DTO de actividad de un analista individual. */
    public record ActividadAnalistaDto(
            String analistaId,
            String nombre,
            long totalEvaluaciones,
            DistribucionDto distribucion,
            double tiempoMedioHorasHabiles,
            boolean isOutlier) {}

    /** DTO de distribución de decisiones. */
    public record DistribucionDto(
            long aprobadas,
            long rechazadas,
            long revisionManual,
            long escaladas,
            BigDecimal tasaAprobacion,
            BigDecimal tasaRechazo,
            BigDecimal tasaManual,
            BigDecimal tasaEscalado) {}

    /** DTO de estadísticas del equipo. */
    public record EstadisticasEquipoDto(
            long totalEvaluaciones,
            double mediaEquipoHorasHabiles,
            double stddevHorasHabiles,
            int numAnalistas,
            boolean outlierDetectionSkipped,
            BigDecimal tasaAprobacionEquipo) {}
}
