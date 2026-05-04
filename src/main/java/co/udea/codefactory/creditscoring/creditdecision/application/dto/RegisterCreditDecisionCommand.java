package co.udea.codefactory.creditscoring.creditdecision.application.dto;

import java.util.UUID;

/**
 * Command que transporta los datos de entrada para registrar una decisión crediticia.
 */
public record RegisterCreditDecisionCommand(
        UUID evaluationId,
        String decision,
        String observations
) {
    public RegisterCreditDecisionCommand {
        if (evaluationId == null) throw new IllegalArgumentException("El evaluationId es obligatorio");
        if (decision == null || decision.isBlank()) throw new IllegalArgumentException("El estado de decisión es obligatorio");
        if (observations == null || observations.isBlank()) throw new IllegalArgumentException("Las observaciones son obligatorias");
    }
}
