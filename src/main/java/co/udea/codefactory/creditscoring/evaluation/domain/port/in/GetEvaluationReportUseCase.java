package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import java.util.UUID;

/**
 * Caso de uso para generar el reporte PDF de una evaluación crediticia.
 */
public interface GetEvaluationReportUseCase {

    /**
     * Genera el reporte PDF de una evaluación.
     *
     * @param evaluationId identificador único de la evaluación
     * @return bytes del PDF generado
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException si no existe
     */
    byte[] generarReporte(UUID evaluationId);
}
