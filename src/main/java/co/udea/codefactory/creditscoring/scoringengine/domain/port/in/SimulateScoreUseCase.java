package co.udea.codefactory.creditscoring.scoringengine.domain.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringengine.domain.model.SimulationScenario;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;

/**
 * Puerto de entrada para el modo de simulación de scoring.
 *
 * <p>La simulación permite calcular puntajes con valores ingresados manualmente,
 * sin necesidad de datos financieros reales y con cualquier modelo (DRAFT o ACTIVE).</p>
 */
public interface SimulateScoreUseCase {

    /** Calcula el puntaje sin persistir el resultado. */
    ScoringResult simular(UUID modeloId, Map<String, BigDecimal> valoresVariables);

    /** Guarda un escenario de simulación para reutilización futura. */
    SimulationScenario guardarEscenario(
            UUID modeloId,
            String nombre,
            String descripcion,
            Map<String, BigDecimal> valoresVariables,
            String creadoPor);

    /** Lista los escenarios guardados para un modelo. */
    List<SimulationScenario> listarEscenarios(UUID modeloId);

    /** Re-ejecuta un escenario guardado previamente. */
    ScoringResult ejecutarEscenario(UUID escenarioId);
}
