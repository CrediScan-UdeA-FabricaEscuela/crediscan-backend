package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import java.util.UUID;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;

/**
 * Caso de uso para obtener una evaluación por su identificador.
 */
public interface GetEvaluationUseCase {

    /**
     * Obtiene una evaluación por su id.
     *
     * @param evaluationId identificador único de la evaluación
     * @return la evaluación encontrada
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException si no existe
     */
    Evaluation obtenerPorId(UUID evaluationId);
}
