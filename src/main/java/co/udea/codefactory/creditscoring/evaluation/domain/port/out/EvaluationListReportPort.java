package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import java.util.List;

import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;

/**
 * Puerto de salida: generación de reporte PDF del listado de evaluaciones.
 */
public interface EvaluationListReportPort {

    /**
     * Genera el contenido binario de un PDF con el listado de evaluaciones dado.
     *
     * @param items    evaluaciones a incluir en el reporte (máximo 1000)
     * @param criteria criterios aplicados (para mostrar en encabezado del PDF)
     * @return bytes del PDF generado
     */
    byte[] generar(List<EvaluationSearchItem> items, EvaluationSearchCriteria criteria);
}
