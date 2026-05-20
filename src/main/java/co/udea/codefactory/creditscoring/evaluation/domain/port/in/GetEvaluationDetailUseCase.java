package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import java.util.UUID;

import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetailView;

/**
 * Caso de uso para obtener el detalle completo de una evaluación,
 * incluyendo nombre del modelo, versión y nombre del solicitante.
 */
public interface GetEvaluationDetailUseCase {

    /**
     * Retorna el detalle enriquecido de una evaluación.
     *
     * @param evaluationId identificador único de la evaluación
     * @return vista de detalle con datos del modelo y solicitante
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException si no existe
     */
    EvaluationDetailView detalle(UUID evaluationId);
}
