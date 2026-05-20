package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;

/**
 * Proyección para la query de estadísticas globales.
 */
public interface OverallStatsProjection {
    long getTotal();
    BigDecimal getAvg();
    BigDecimal getStddev();
}
