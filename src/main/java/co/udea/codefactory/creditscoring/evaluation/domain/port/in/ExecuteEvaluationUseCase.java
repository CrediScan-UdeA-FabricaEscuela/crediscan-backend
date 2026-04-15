package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import co.udea.codefactory.creditscoring.evaluation.application.dto.ExecuteEvaluationCommand;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;

/**
 * Caso de uso principal: ejecutar una evaluación crediticia para un solicitante.
 */
public interface ExecuteEvaluationUseCase {

    /**
     * Ejecuta la evaluación de riesgo crediticio según el command recibido.
     *
     * @param command datos del solicitante y modelo a usar
     * @return la evaluación persistida con su resultado y desglose completo
     */
    Evaluation ejecutar(ExecuteEvaluationCommand command);
}
