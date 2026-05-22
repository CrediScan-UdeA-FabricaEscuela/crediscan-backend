package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence.efectividad;

/**
 * Proyección de la query de la matriz de confusión para HU-016.
 * Devuelve el nivel de riesgo como String (columna en BD es varchar)
 * y el nombre de la decisión junto con el conteo.
 */
public interface MatrizRawProjection {
    /** Valor de la columna {@code risk_level} en la tabla evaluation. */
    String getRiskLevel();

    /** Valor de la columna {@code decision} en la tabla credit_decision. */
    String getDecision();

    /** Cantidad de evaluaciones para esta combinación risk_level × decision. */
    long getCount();
}
