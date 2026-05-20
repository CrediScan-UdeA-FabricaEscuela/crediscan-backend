package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

/**
 * Proyección para el conteo de evaluaciones agrupadas por nivel de riesgo.
 */
public interface RiskLevelCountProjection {

    /** Nombre del nivel de riesgo (valor del enum {@code RiskLevel}). */
    String getLevel();

    /** Total de evaluaciones para ese nivel. */
    long getTotal();
}
