package co.udea.codefactory.creditscoring.scoringengine.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CalculateScoreRequest(

        @NotNull(message = "El identificador del solicitante es obligatorio")
        UUID aplicanteId,

        /** Si es nulo, se usa el modelo activo. */
        UUID modeloId
) {}
