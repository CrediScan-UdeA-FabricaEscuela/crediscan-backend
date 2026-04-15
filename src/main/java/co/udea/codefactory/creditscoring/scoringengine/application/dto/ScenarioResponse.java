package co.udea.codefactory.creditscoring.scoringengine.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringengine.domain.model.SimulationScenario;

/** Representación de un escenario de simulación en la respuesta de la API. */
public record ScenarioResponse(
        UUID id,
        UUID modeloId,
        String nombre,
        String descripcion,
        Map<String, BigDecimal> valoresVariables,
        OffsetDateTime fechaCreacion,
        String creadoPor) {

    public static ScenarioResponse from(SimulationScenario escenario) {
        return new ScenarioResponse(
                escenario.id(),
                escenario.modeloId(),
                escenario.nombre(),
                escenario.descripcion(),
                escenario.valoresVariables(),
                escenario.fechaCreacion(),
                escenario.creadoPor());
    }
}
