package co.udea.codefactory.creditscoring.scoringengine.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Escenario de simulación de scoring guardado por el usuario.
 *
 * <p>Almacena el conjunto de valores ingresados manualmente para poder
 * re-ejecutar el cálculo en cualquier momento sin necesidad de datos financieros reales.</p>
 */
public record SimulationScenario(
        UUID id,
        UUID modeloId,
        String nombre,
        String descripcion,
        Map<String, BigDecimal> valoresVariables,
        OffsetDateTime fechaCreacion,
        String creadoPor) {

    public SimulationScenario {
        if (modeloId == null) {
            throw new IllegalArgumentException("El modeloId del escenario es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del escenario es obligatorio");
        }
        if (valoresVariables == null || valoresVariables.isEmpty()) {
            throw new IllegalArgumentException("El escenario debe incluir al menos un valor de variable");
        }
        valoresVariables = Map.copyOf(valoresVariables);
    }

    /** Crea un nuevo escenario con UUID generado. */
    public static SimulationScenario crear(
            UUID modeloId,
            String nombre,
            String descripcion,
            Map<String, BigDecimal> valoresVariables,
            String creadoPor) {
        return new SimulationScenario(
                UUID.randomUUID(), modeloId, nombre, descripcion,
                valoresVariables, OffsetDateTime.now(), creadoPor);
    }

    /** Reconstituye un escenario existente desde el repositorio. */
    public static SimulationScenario rehydrate(
            UUID id, UUID modeloId, String nombre, String descripcion,
            Map<String, BigDecimal> valoresVariables,
            OffsetDateTime fechaCreacion, String creadoPor) {
        return new SimulationScenario(
                id, modeloId, nombre, descripcion,
                valoresVariables, fechaCreacion, creadoPor);
    }
}
