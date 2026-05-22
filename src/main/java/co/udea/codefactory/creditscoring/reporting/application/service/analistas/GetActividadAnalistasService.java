package co.udea.codefactory.creditscoring.reporting.application.service.analistas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.reporting.application.util.BusinessHoursCalculator;
import co.udea.codefactory.creditscoring.reporting.application.util.OutlierDetector;
import co.udea.codefactory.creditscoring.reporting.application.util.OutlierDetector.OutlierResult;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalista;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.DistribucionDecisiones;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.EstadisticasEquipo;
import co.udea.codefactory.creditscoring.reporting.domain.port.in.analistas.GetActividadAnalistasUseCase;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.ActividadAnalistasQueryPort;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.ActividadAnalistasQueryPort.AnalistaCountsAggregate;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.ActividadAnalistasQueryPort.AnalistaTimestampAggregate;
import lombok.RequiredArgsConstructor;

/**
 * Servicio de aplicación para el reporte de actividad de analistas (HU-017).
 * <p>
 * Realiza dos consultas: conteos por analista y timestamps para el cálculo de
 * tiempo en horas hábiles. La detección de outliers usa desviación estándar
 * poblacional sobre los tiempos medios del equipo.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class GetActividadAnalistasService implements GetActividadAnalistasUseCase {

    private final ActividadAnalistasQueryPort queryPort;
    private final BusinessHoursCalculator businessHoursCalculator;

    /** Mínimo de evaluaciones para que un analista califique en detección de outliers (RN2-017). */
    private static final int MIN_EVALUACIONES_CALIFICADO = 10;

    @Override
    @Transactional(readOnly = true)
    public ActividadAnalistasReporte reporte(OffsetDateTime desde, OffsetDateTime hasta) {
        validarRango(desde, hasta);

        List<AnalistaCountsAggregate> conteos = queryPort.queryCounts(desde, hasta);
        if (conteos.isEmpty()) {
            return ActividadAnalistasReporte.empty(desde, hasta);
        }

        // Calcular tiempos medios en horas hábiles por analista
        List<AnalistaTimestampAggregate> timestamps = queryPort.queryTimestamps(desde, hasta);
        Map<String, Double> tiemposPorAnalista = calcularTiemposMedios(timestamps);

        // Filtrar analistas calificados (totalEvaluaciones >= 10) para outlier detection (RN2-017)
        Map<String, Double> tiemposCalificados = conteos.stream()
                .filter(c -> c.total() >= MIN_EVALUACIONES_CALIFICADO)
                .filter(c -> tiemposPorAnalista.containsKey(c.evaluatedBy()))
                .collect(Collectors.toMap(
                        AnalistaCountsAggregate::evaluatedBy,
                        c -> tiemposPorAnalista.getOrDefault(c.evaluatedBy(), 0.0)));

        // Detección de outliers solo sobre analistas calificados
        OutlierResult outlierResult = OutlierDetector.detect(tiemposCalificados);

        // Construir lista de analistas con sus métricas
        List<ActividadAnalista> analistas = buildAnalistas(conteos, tiemposPorAnalista, outlierResult);

        // Estadísticas del equipo
        EstadisticasEquipo estadisticas = buildEstadisticas(conteos, tiemposCalificados, outlierResult);

        return new ActividadAnalistasReporte(analistas, estadisticas, true, desde, hasta);
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private void validarRango(OffsetDateTime desde, OffsetDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "El parámetro 'desde' (" + desde + ") no puede ser posterior a 'hasta' (" + hasta + ")");
        }
    }

    /**
     * Calcula el tiempo medio en horas hábiles por analista.
     */
    private Map<String, Double> calcularTiemposMedios(List<AnalistaTimestampAggregate> timestamps) {
        Map<String, List<Double>> tiemposPorAnalista = new HashMap<>();

        for (AnalistaTimestampAggregate ts : timestamps) {
            if (ts.evaluatedAt() == null || ts.decidedAt() == null) continue;
            double horas = businessHoursCalculator.calcular(ts.evaluatedAt(), ts.decidedAt());
            tiemposPorAnalista.computeIfAbsent(ts.evaluatedBy(), k -> new ArrayList<>()).add(horas);
        }

        Map<String, Double> medios = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : tiemposPorAnalista.entrySet()) {
            double promedio = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            medios.put(entry.getKey(), promedio);
        }
        return medios;
    }

    /**
     * Construye la lista de métricas por analista.
     */
    private List<ActividadAnalista> buildAnalistas(
            List<AnalistaCountsAggregate> conteos,
            Map<String, Double> tiemposPorAnalista,
            OutlierResult outlierResult) {

        return conteos.stream().map(c -> {
            DistribucionDecisiones distribucion = calcularDistribucion(c);
            double tiempoMedio = tiemposPorAnalista.getOrDefault(c.evaluatedBy(), 0.0);
            // Solo los analistas calificados (>= 10 eval) pueden ser outliers (RN2-017)
            boolean isOutlier = !outlierResult.skipped()
                    && outlierResult.outlierIds().contains(c.evaluatedBy());
            // nombre: fallback a username — AppUser no tiene campo de nombre de pila (CA1-017)
            String nombre = c.evaluatedBy();
            return new ActividadAnalista(
                    c.evaluatedBy(),
                    nombre,
                    c.total(),
                    distribucion,
                    tiempoMedio,
                    isOutlier);
        }).toList();
    }

    /**
     * Calcula la distribución de decisiones con porcentajes para un analista.
     */
    private DistribucionDecisiones calcularDistribucion(AnalistaCountsAggregate c) {
        long total = c.total();
        BigDecimal totalBd = BigDecimal.valueOf(total);

        BigDecimal pctAprobacion = total == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(c.aprobadas() * 100L).divide(totalBd, 2, RoundingMode.HALF_UP);
        BigDecimal pctRechazo = total == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(c.rechazadas() * 100L).divide(totalBd, 2, RoundingMode.HALF_UP);
        BigDecimal pctManual = total == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(c.revisionManual() * 100L).divide(totalBd, 2, RoundingMode.HALF_UP);
        BigDecimal pctEscalado = total == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(c.escaladas() * 100L).divide(totalBd, 2, RoundingMode.HALF_UP);

        return new DistribucionDecisiones(
                c.aprobadas(), c.rechazadas(), c.revisionManual(), c.escaladas(),
                pctAprobacion, pctRechazo, pctManual, pctEscalado);
    }

    /**
     * Construye las estadísticas agregadas del equipo.
     *
     * <p>Los campos de media/stddev y {@code tasaAprobacionEquipo} se calculan
     * exclusivamente sobre los analistas calificados (totalEvaluaciones &ge; 10),
     * consistente con el filtro aplicado en la detección de outliers (RN2-017, CA4-017).</p>
     *
     * @param conteos             todos los analistas con evaluaciones en el período
     * @param tiemposCalificados  mapa de tiempos medios de los analistas calificados
     * @param outlierResult       resultado de la detección de outliers
     */
    private EstadisticasEquipo buildEstadisticas(
            List<AnalistaCountsAggregate> conteos,
            Map<String, Double> tiemposCalificados,
            OutlierResult outlierResult) {

        // totalEvaluaciones y numAnalistas abarcan TODO el equipo (no solo calificados)
        long totalEquipo = conteos.stream().mapToLong(AnalistaCountsAggregate::total).sum();
        int numAnalistas = conteos.size();

        // Media y stddev calculadas sobre los analistas calificados
        double[] tiempos = tiemposCalificados.values().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        double media = tiempos.length == 0 ? 0.0
                : java.util.Arrays.stream(tiempos).average().orElse(0.0);

        double varianza = tiempos.length == 0 ? 0.0
                : java.util.Arrays.stream(tiempos)
                        .map(t -> (t - media) * (t - media))
                        .average()
                        .orElse(0.0);

        double stddev = Math.sqrt(varianza);

        // tasaAprobacionEquipo = aprobadas / total sobre analistas calificados (CA4-017)
        BigDecimal tasaAprobacionEquipo = calcularTasaAprobacionEquipo(conteos);

        return new EstadisticasEquipo(
                totalEquipo, media, stddev, numAnalistas, outlierResult.skipped(), tasaAprobacionEquipo);
    }

    /**
     * Calcula la tasa de aprobación global del equipo considerando solo los analistas
     * con totalEvaluaciones &ge; 10 (CA4-017).
     */
    private BigDecimal calcularTasaAprobacionEquipo(List<AnalistaCountsAggregate> conteos) {
        List<AnalistaCountsAggregate> calificados = conteos.stream()
                .filter(c -> c.total() >= MIN_EVALUACIONES_CALIFICADO)
                .toList();

        if (calificados.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }

        long totalAprobaciones = calificados.stream().mapToLong(AnalistaCountsAggregate::aprobadas).sum();
        long totalDecisiones   = calificados.stream().mapToLong(AnalistaCountsAggregate::total).sum();

        if (totalDecisiones == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return BigDecimal.valueOf(totalAprobaciones * 100L)
                .divide(BigDecimal.valueOf(totalDecisiones), 2, RoundingMode.HALF_UP);
    }
}
