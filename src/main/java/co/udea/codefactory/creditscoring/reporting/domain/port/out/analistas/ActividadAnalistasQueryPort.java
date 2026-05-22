package co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Puerto de salida para las consultas analíticas de actividad de analistas (HU-017).
 * <p>
 * Usa dos queries separadas: una para los conteos de decisiones y otra para los
 * timestamps necesarios para calcular el tiempo en horas hábiles en Java.
 * </p>
 */
public interface ActividadAnalistasQueryPort {

    /**
     * Retorna los conteos de decisiones agrupados por analista.
     * Solo incluye evaluaciones con credit_decision (INNER JOIN).
     *
     * @param desde inicio del rango (inclusive)
     * @param hasta fin del rango (inclusive)
     * @return lista de agregados de conteo por analista
     */
    List<AnalistaCountsAggregate> queryCounts(OffsetDateTime desde, OffsetDateTime hasta);

    /**
     * Retorna los timestamps para el cálculo de tiempo en horas hábiles.
     * Solo incluye evaluaciones con credit_decision (INNER JOIN).
     *
     * @param desde inicio del rango (inclusive)
     * @param hasta fin del rango (inclusive)
     * @return lista de timestamps por evaluación
     */
    List<AnalistaTimestampAggregate> queryTimestamps(OffsetDateTime desde, OffsetDateTime hasta);

    /**
     * Agregado de conteos por analista.
     */
    record AnalistaCountsAggregate(
            String evaluatedBy,
            long total,
            long aprobadas,
            long rechazadas,
            long revisionManual,
            long escaladas) {}

    /**
     * Timestamps de una evaluación y su decisión.
     */
    record AnalistaTimestampAggregate(
            String evaluatedBy,
            OffsetDateTime evaluatedAt,
            OffsetDateTime decidedAt) {}
}
