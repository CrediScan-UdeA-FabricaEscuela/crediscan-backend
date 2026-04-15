package co.udea.codefactory.creditscoring.scoringengine.application.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud de simulación de scoring con valores ingresados manualmente.
 *
 * @param modeloId          UUID del modelo a usar; si es null se usa el modelo activo
 * @param valoresVariables  mapa de nombre_variable → valor numérico observado
 */
public record SimulateScoreRequest(
        UUID modeloId,
        @NotNull @NotEmpty Map<String, BigDecimal> valoresVariables) {
}
