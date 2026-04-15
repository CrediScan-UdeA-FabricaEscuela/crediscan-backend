package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para la ejecución de una evaluación crediticia.
 * El modelId es opcional — si es null se usa el modelo activo del sistema.
 */
public record ExecuteEvaluationRequest(

        @NotNull(message = "El identificador del solicitante es obligatorio")
        UUID applicantId,

        /** Si es nulo, se usa el modelo activo. */
        UUID modelId
) {}
