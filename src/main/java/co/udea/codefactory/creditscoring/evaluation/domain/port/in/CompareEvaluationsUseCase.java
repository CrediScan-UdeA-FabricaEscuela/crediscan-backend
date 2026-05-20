package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import java.util.UUID;

import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationComparison;

/**
 * Caso de uso para comparar dos evaluaciones del mismo solicitante.
 */
public interface CompareEvaluationsUseCase {

    /**
     * Compara dos evaluaciones y calcula el delta de puntaje.
     * Ambas evaluaciones deben pertenecer al mismo solicitante.
     *
     * @param eval1Id identificador de la primera evaluación
     * @param eval2Id identificador de la segunda evaluación
     * @return comparación con delta = eval2.totalScore - eval1.totalScore
     * @throws co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException si son de distintos solicitantes
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException              si alguna evaluación no existe
     */
    EvaluationComparison comparar(UUID eval1Id, UUID eval2Id);
}
