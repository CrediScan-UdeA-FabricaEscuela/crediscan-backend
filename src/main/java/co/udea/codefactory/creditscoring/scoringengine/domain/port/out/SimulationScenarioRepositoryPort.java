package co.udea.codefactory.creditscoring.scoringengine.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringengine.domain.model.SimulationScenario;

/** Puerto de salida para la persistencia de escenarios de simulación. */
public interface SimulationScenarioRepositoryPort {

    SimulationScenario save(SimulationScenario escenario);

    Optional<SimulationScenario> findById(UUID id);

    List<SimulationScenario> findByModeloId(UUID modeloId);

    void deleteById(UUID id);
}
