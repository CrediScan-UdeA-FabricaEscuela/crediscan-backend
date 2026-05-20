package co.udea.codefactory.creditscoring.reporting.domain.port.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Puerto de salida para consultar los datos de distribución de riesgo desde persistence.
 * Recibe tipoEmpleo como String (apiValue) para mantener el dominio agnóstico del enum cross-BC.
 */
public interface RiskDistributionQueryPort {

    /**
     * Retorna los conteos y score promedio por nivel de riesgo en el rango dado.
     * Cuenta una sola evaluación por solicitante (la más reciente).
     */
    List<LevelAggregate> distributionByLevel(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            String tipoEmpleo);

    /**
     * Retorna los conteos por bin de histograma en el rango dado.
     * Cuenta una sola evaluación por solicitante (la más reciente).
     */
    List<BinAggregate> histogram(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            String tipoEmpleo);

    /**
     * Retorna las estadísticas globales: total, score promedio y desviación estándar.
     * Cuenta una sola evaluación por solicitante (la más reciente).
     */
    OverallAggregate overallStats(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            String tipoEmpleo);

    // -------------------------------------------------------------------------
    // Inner records
    // -------------------------------------------------------------------------

    record LevelAggregate(RiskLevel level, long count, BigDecimal avgScore) {}

    record BinAggregate(int binStart, long count) {}

    record OverallAggregate(long total, BigDecimal avg, BigDecimal stddev) {}
}
