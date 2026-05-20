package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import java.util.List;
import java.util.UUID;

import co.udea.codefactory.creditscoring.evaluation.domain.model.HistoryItem;

/**
 * Caso de uso para obtener el historial de evaluaciones de un solicitante,
 * ordenado por fecha descendente con cálculo de delta de puntaje.
 */
public interface GetApplicantEvaluationHistoryUseCase {

    /**
     * Retorna el historial de evaluaciones del solicitante ordenado por fecha DESC.
     * Si el solicitante no tiene evaluaciones retorna una lista vacía.
     *
     * @param applicantId identificador del solicitante
     * @return lista de ítems de historial (puede ser vacía)
     */
    List<HistoryItem> historial(UUID applicantId);
}
