package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;

/**
 * Proyección para la query de distribución por nivel de riesgo.
 */
public interface LevelCountProjection {
    String getLevel();
    long getCnt();
    BigDecimal getAvgScore();
}
