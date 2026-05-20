package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

/**
 * Puerto de entrada: búsqueda avanzada paginada de evaluaciones crediticias.
 */
public interface SearchEvaluationsUseCase {

    /**
     * Busca evaluaciones aplicando los criterios dados y retorna una página de resultados.
     *
     * @param criteria criterios de filtrado (fechas requeridas + filtros opcionales)
     * @param page     parámetros de paginación (page index 0-based, size máximo 100)
     * @return página de items de evaluación
     */
    PagedResult<EvaluationSearchItem> search(EvaluationSearchCriteria criteria, PageRequest page);
}
