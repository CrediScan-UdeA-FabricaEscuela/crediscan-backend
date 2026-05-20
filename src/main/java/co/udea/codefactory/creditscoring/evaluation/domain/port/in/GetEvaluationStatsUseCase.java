package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationStats;

/**
 * Puerto de entrada: estadísticas agregadas del conjunto filtrado de evaluaciones.
 */
public interface GetEvaluationStatsUseCase {

    /**
     * Calcula total, promedio de puntaje y distribución por nivel para el criterio dado.
     *
     * @param criteria criterios de filtrado (los mismos que en búsqueda)
     * @return estadísticas del conjunto filtrado
     */
    EvaluationStats stats(EvaluationSearchCriteria criteria);
}
