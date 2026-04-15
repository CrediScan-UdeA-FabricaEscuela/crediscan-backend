package co.udea.codefactory.creditscoring.scoringengine.application.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Solicitud para guardar un escenario de simulación. */
public record SaveScenarioRequest(
        @NotNull UUID modeloId,
        @NotBlank @Size(max = 200) String nombre,
        @Size(max = 500) String descripcion,
        @NotNull @NotEmpty Map<String, BigDecimal> valoresVariables) {
}
