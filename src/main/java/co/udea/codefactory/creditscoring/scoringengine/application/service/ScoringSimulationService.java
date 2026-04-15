package co.udea.codefactory.creditscoring.scoringengine.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.scoringengine.domain.model.SimulationScenario;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.in.SimulateScoreUseCase;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.out.SimulationScenarioRepositoryPort;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

/**
 * Servicio de simulación de scoring en modo prueba.
 *
 * <p>Permite calcular puntajes con valores ingresados manualmente, sin consumir
 * datos financieros reales y sin restricción de estado del modelo (DRAFT o ACTIVE).</p>
 */
@Service
@Transactional(readOnly = true)
public class ScoringSimulationService implements SimulateScoreUseCase {

    private final ScoringModelRepositoryPort modeloRepo;
    private final SimulationScenarioRepositoryPort scenarioRepo;
    private final ScoringCalculator calculator;

    public ScoringSimulationService(
            ScoringModelRepositoryPort modeloRepo,
            SimulationScenarioRepositoryPort scenarioRepo,
            ScoringCalculator calculator) {
        this.modeloRepo = modeloRepo;
        this.scenarioRepo = scenarioRepo;
        this.calculator = calculator;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','RISK_MANAGER')")
    public ScoringResult simular(UUID modeloId, Map<String, BigDecimal> valoresVariables) {
        ScoringModel modelo = resolverModelo(modeloId);
        // contextoId = modeloId (no hay solicitante en simulación)
        return calculator.calcular(modelo, modelo.id(), valoresVariables);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','RISK_MANAGER')")
    public SimulationScenario guardarEscenario(
            UUID modeloId,
            String nombre,
            String descripcion,
            Map<String, BigDecimal> valoresVariables,
            String creadoPor) {
        // Verifica que el modelo exista antes de guardar
        resolverModelo(modeloId);
        SimulationScenario escenario = SimulationScenario.crear(
                modeloId, nombre, descripcion, valoresVariables, creadoPor);
        return scenarioRepo.save(escenario);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','RISK_MANAGER')")
    public List<SimulationScenario> listarEscenarios(UUID modeloId) {
        return scenarioRepo.findByModeloId(modeloId);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','RISK_MANAGER')")
    public ScoringResult ejecutarEscenario(UUID escenarioId) {
        SimulationScenario escenario = scenarioRepo.findById(escenarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Escenario de simulación no encontrado: " + escenarioId));
        ScoringModel modelo = resolverModelo(escenario.modeloId());
        return calculator.calcular(modelo, escenario.id(), escenario.valoresVariables());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resuelve el modelo por ID sin restricción de estado.
     * La simulación puede usar modelos en DRAFT o ACTIVE (CA5).
     */
    private ScoringModel resolverModelo(UUID modeloId) {
        if (modeloId != null) {
            return modeloRepo.findById(modeloId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Modelo de scoring no encontrado: " + modeloId));
        }
        return modeloRepo.findActive()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay un modelo de scoring activo en el sistema"));
    }
}
