package co.udea.codefactory.creditscoring.reporting.domain.model.analistas;

import java.math.BigDecimal;

/**
 * Estadísticas agregadas del equipo de analistas para el período consultado (HU-017).
 *
 * @param totalEvaluacionesEquipo  total de evaluaciones con decisión de todos los analistas
 * @param mediaEquipoHorasHabiles  tiempo medio del equipo en horas hábiles
 * @param stddevHorasHabiles       desviación estándar poblacional del tiempo medio del equipo
 * @param numAnalistas             número de analistas con evaluaciones en el período
 * @param outlierDetectionSkipped  true si la detección fue omitida (N &lt; 3 calificados)
 * @param tasaAprobacionEquipo     tasa de aprobación global del equipo, calculada únicamente
 *                                 sobre los analistas con totalEvaluaciones &ge; 10 (CA4-017)
 */
public record EstadisticasEquipo(
        long totalEvaluacionesEquipo,
        double mediaEquipoHorasHabiles,
        double stddevHorasHabiles,
        int numAnalistas,
        boolean outlierDetectionSkipped,
        BigDecimal tasaAprobacionEquipo
) {}
