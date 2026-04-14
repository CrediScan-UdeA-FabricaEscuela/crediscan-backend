package co.udea.codefactory.creditscoring.scoringmodel.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos de entrada para crear una nueva versión del modelo de scoring.
 * Si {@code clonarDesde} no es nulo, el nuevo modelo copia las variables
 * del modelo origen (CA1). En caso contrario, se populan desde las
 * variables de scoring activas con sus pesos actuales.
 */
public record CreateScoringModelRequest(

        @NotBlank(message = "El nombre de la versión del modelo es obligatorio")
        String nombre,

        String descripcion,

        UUID clonarDesde) {
}
