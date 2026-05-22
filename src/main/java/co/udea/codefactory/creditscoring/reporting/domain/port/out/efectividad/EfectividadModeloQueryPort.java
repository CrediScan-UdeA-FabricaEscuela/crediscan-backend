package co.udea.codefactory.creditscoring.reporting.domain.port.out.efectividad;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Puerto de salida para la consulta analítica de la matriz de confusión (HU-016).
 * <p>
 * El adaptador implementa la query nativa {@code evaluation INNER JOIN credit_decision}.
 * El dominio no conoce la implementación de persistencia.
 * </p>
 */
public interface EfectividadModeloQueryPort {

    /**
     * Retorna las combinaciones risk_level × decision con sus conteos.
     * Solo incluye evaluaciones que tienen una {@code credit_decision} asociada.
     *
     * @param desde      inicio del rango (inclusive)
     * @param hasta      fin del rango (inclusive)
     * @param analistaId filtro por analista (null = todos)
     * @return lista de agregados crudos desde la base de datos
     */
    List<MatrizAggregate> queryMatriz(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            String analistaId);

    /**
     * Agregado crudo: una fila devuelta por la query de la matriz.
     *
     * @param riskLevel nombre del nivel de riesgo (String, valor de la columna DB)
     * @param decision  nombre de la decisión (String, valor de la columna DB)
     * @param count     conteo de evaluaciones
     */
    record MatrizAggregate(String riskLevel, String decision, long count) {}
}
