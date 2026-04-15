package co.udea.codefactory.creditscoring.scoringengine.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.scoringengine.domain.model.SimulationScenario;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.out.SimulationScenarioRepositoryPort;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelStatus;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ScoringSimulationServiceTest {

    @Mock private ScoringModelRepositoryPort modeloRepo;
    @Mock private SimulationScenarioRepositoryPort scenarioRepo;
    @Mock private ScoringCalculator calculator;

    @InjectMocks
    private ScoringSimulationService service;

    private static final UUID MODELO_ID = UUID.randomUUID();
    private static final UUID ESCENARIO_ID = UUID.randomUUID();

    private static final Map<String, BigDecimal> VALORES = Map.of(
            "moras_12_meses", BigDecimal.ZERO,
            "score_buro", new BigDecimal("720"));

    // =========================================================================
    // simular()
    // =========================================================================

    @Test
    void simular_modeloExiste_delegaAlCalculator() {
        ScoringModel modelo = modeloDraft();
        ScoringResult resultadoEsperado = resultadoAprobado();

        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.of(modelo));
        when(calculator.calcular(eq(modelo), eq(MODELO_ID), eq(VALORES)))
                .thenReturn(resultadoEsperado);

        ScoringResult resultado = service.simular(MODELO_ID, VALORES);

        assertThat(resultado.rechazadoPorKo()).isFalse();
        verify(calculator).calcular(modelo, MODELO_ID, VALORES);
    }

    @Test
    void simular_modeloNoExiste_lanzaResourceNotFoundException() {
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simular(MODELO_ID, VALORES))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void simular_modeloDraft_esValido() {
        // La simulación permite modelos en DRAFT (CA5 del HU-009)
        ScoringModel modeloDraft = modeloDraft();
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.of(modeloDraft));
        when(calculator.calcular(any(), any(), any())).thenReturn(resultadoAprobado());

        ScoringResult resultado = service.simular(MODELO_ID, VALORES);

        assertThat(resultado).isNotNull();
    }

    // =========================================================================
    // guardarEscenario()
    // =========================================================================

    @Test
    void guardarEscenario_datosValidos_guardaYRetornaEscenario() {
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.of(modeloDraft()));
        SimulationScenario escenario = escenarioGuardado();
        when(scenarioRepo.save(any())).thenReturn(escenario);

        SimulationScenario resultado = service.guardarEscenario(
                MODELO_ID, "Test A", "Descripción", VALORES, "user");

        assertThat(resultado.nombre()).isEqualTo("Escenario Test");
        verify(scenarioRepo).save(any(SimulationScenario.class));
    }

    @Test
    void guardarEscenario_modeloNoExiste_lanzaResourceNotFoundException() {
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.guardarEscenario(
                MODELO_ID, "Test", null, VALORES, "user"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // listarEscenarios()
    // =========================================================================

    @Test
    void listarEscenarios_retornaListaDelRepositorio() {
        when(scenarioRepo.findByModeloId(MODELO_ID)).thenReturn(List.of(escenarioGuardado()));

        List<SimulationScenario> lista = service.listarEscenarios(MODELO_ID);

        assertThat(lista).hasSize(1);
    }

    // =========================================================================
    // ejecutarEscenario()
    // =========================================================================

    @Test
    void ejecutarEscenario_escenarioExiste_retornaResultado() {
        SimulationScenario escenario = escenarioGuardado();
        when(scenarioRepo.findById(ESCENARIO_ID)).thenReturn(Optional.of(escenario));
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.of(modeloDraft()));
        when(calculator.calcular(any(), eq(ESCENARIO_ID), eq(VALORES)))
                .thenReturn(resultadoAprobado());

        ScoringResult resultado = service.ejecutarEscenario(ESCENARIO_ID);

        assertThat(resultado.rechazadoPorKo()).isFalse();
        verify(calculator).calcular(any(ScoringModel.class), eq(ESCENARIO_ID), eq(VALORES));
    }

    @Test
    void ejecutarEscenario_escenarioNoExiste_lanzaResourceNotFoundException() {
        when(scenarioRepo.findById(ESCENARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ejecutarEscenario(ESCENARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Escenario");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ScoringModel modeloDraft() {
        return ScoringModel.rehydrate(MODELO_ID, "Modelo Draft", "Desc", 1,
                ModelStatus.DRAFT, List.of(), OffsetDateTime.now(), null);
    }

    private SimulationScenario escenarioGuardado() {
        return SimulationScenario.rehydrate(
                ESCENARIO_ID, MODELO_ID, "Escenario Test", "Desc",
                VALORES, OffsetDateTime.now(), "user");
    }

    private ScoringResult resultadoAprobado() {
        return ScoringResult.aprobado(
                MODELO_ID, MODELO_ID, new BigDecimal("75.00"),
                List.of(), List.of());
    }
}
