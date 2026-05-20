package co.udea.codefactory.creditscoring.reporting.domain.model;

import java.math.BigDecimal;

/**
 * Estadísticas globales del reporte: promedio, desviación estándar y total de evaluaciones.
 */
public record OverallStats(
        BigDecimal averageScore,
        BigDecimal stdDev,
        long totalEvaluations
) {
    /**
     * Retorna una instancia con todos los valores en cero.
     * Se usa cuando no hay datos en el rango solicitado.
     */
    public static OverallStats zero() {
        return new OverallStats(
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                0L);
    }
}
