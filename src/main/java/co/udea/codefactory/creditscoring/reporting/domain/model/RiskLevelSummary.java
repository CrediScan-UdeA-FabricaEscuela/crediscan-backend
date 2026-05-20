package co.udea.codefactory.creditscoring.reporting.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Fila de la tabla resumen por nivel de riesgo.
 * Contiene nivel, conteo, porcentaje y score promedio.
 */
public record RiskLevelSummary(
        RiskLevel level,
        long count,
        BigDecimal percentage,
        BigDecimal averageScore
) {
    public RiskLevelSummary {
        Objects.requireNonNull(level, "level no puede ser null");
        if (count < 0) {
            throw new IllegalArgumentException("count debe ser >= 0");
        }
        Objects.requireNonNull(percentage, "percentage no puede ser null");
        if (percentage.signum() < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("percentage debe estar en [0, 100]");
        }
        Objects.requireNonNull(averageScore, "averageScore no puede ser null");
    }
}
