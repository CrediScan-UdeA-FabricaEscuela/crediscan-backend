package co.udea.codefactory.creditscoring.evaluation.application.dto;

import java.util.UUID;

/**
 * Command para ejecutar una evaluación crediticia.
 * El modeloId es nullable — si es null se usa el modelo activo.
 */
public record ExecuteEvaluationCommand(UUID applicantId, UUID modelId) {

    public ExecuteEvaluationCommand {
        if (applicantId == null) {
            throw new IllegalArgumentException("El applicantId es obligatorio");
        }
    }
}
