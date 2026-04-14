package co.udea.codefactory.creditscoring.scoringmodel.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.CreateScoringModelRequest;
import co.udea.codefactory.creditscoring.scoringmodel.domain.exception.ScoringModelValidationException;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelStatus;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableType;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ScoringModelCommandServiceTest {

    @Mock
    private ScoringModelRepositoryPort modeloRepo;

    @Mock
    private ScoringVariableRepositoryPort variableRepo;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ScoringModelCommandService service;

    private static final UUID MODELO_ID = UUID.randomUUID();

    // =========================================================================
    // crear()
    // =========================================================================

    @Test
    void crear_conNombreDuplicado_lanzaExcepcion() {
        when(modeloRepo.existsByNombre("v1")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CreateScoringModelRequest("v1", null, null)))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("v1");

        verify(modeloRepo, never()).save(any());
    }

    @Test
    void crear_sinClonar_usaVariablesActivas() {
        when(modeloRepo.existsByNombre("Modelo fresco")).thenReturn(false);
        when(modeloRepo.maxVersion()).thenReturn(0);
        when(variableRepo.findAllActivas()).thenReturn(List.of(
                variableActiva(new BigDecimal("0.60")),
                variableActiva(new BigDecimal("0.40"))));
        when(modeloRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScoringModel resultado = service.crear(new CreateScoringModelRequest("Modelo fresco", null, null));

        assertThat(resultado.estado()).isEqualTo(ModelStatus.DRAFT);
        assertThat(resultado.version()).isEqualTo(1);
        assertThat(resultado.variables()).hasSize(2);
        verify(variableRepo).findAllActivas();
    }

    @Test
    void crear_clonarDesdeModeloInexistente_lanzaResourceNotFoundException() {
        UUID origenId = UUID.randomUUID();
        when(modeloRepo.existsByNombre("Clon")).thenReturn(false);
        when(modeloRepo.findById(origenId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(new CreateScoringModelRequest("Clon", null, origenId)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(modeloRepo, never()).save(any());
    }

    @Test
    void crear_clonarDesdeModeloExistente_copiaVariables() {
        UUID origenId = UUID.randomUUID();
        ScoringModel origen = modeloConVariables(origenId, 2);
        when(modeloRepo.existsByNombre("Clon v2")).thenReturn(false);
        when(modeloRepo.findById(origenId)).thenReturn(Optional.of(origen));
        when(modeloRepo.maxVersion()).thenReturn(1);
        when(modeloRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScoringModel resultado = service.crear(new CreateScoringModelRequest("Clon v2", null, origenId));

        assertThat(resultado.variables()).hasSize(2);
        assertThat(resultado.version()).isEqualTo(2);
        verify(variableRepo, never()).findAllActivas();
    }

    // =========================================================================
    // activar()
    // =========================================================================

    @Test
    void activar_conIdInexistente_lanzaResourceNotFoundException() {
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activar(MODELO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activar_conVariablesInvalidas_propagaExcepcionDeDominio() {
        // Solo 2 variables — viola RN2
        ScoringModel draft = modeloConVariables(MODELO_ID, 2);
        when(modeloRepo.findById(MODELO_ID)).thenReturn(Optional.of(draft));
        when(variableRepo.findById(any())).thenReturn(Optional.of(variableActiva(new BigDecimal("0.50"))));

        assertThatThrownBy(() -> service.activar(MODELO_ID))
                .isInstanceOf(ScoringModelValidationException.class);

        verify(modeloRepo, never()).update(any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ScoringModel modeloConVariables(UUID id, int cantidad) {
        List<ModelVariable> variables = new java.util.ArrayList<>();
        BigDecimal peso = BigDecimal.ONE.divide(BigDecimal.valueOf(cantidad), 4, java.math.RoundingMode.HALF_UP);
        for (int i = 0; i < cantidad; i++) {
            variables.add(new ModelVariable(UUID.randomUUID(), id, UUID.randomUUID(), peso, null));
        }
        return ScoringModel.rehydrate(id, "Modelo", "Desc", 1, ModelStatus.DRAFT,
                variables, java.time.OffsetDateTime.now(), null);
    }

    private ScoringVariable variableActiva(BigDecimal peso) {
        List<VariableRange> rangos = List.of(
                new VariableRange(UUID.randomUUID(), null, BigDecimal.ZERO, new BigDecimal("100"), 50, null));
        return ScoringVariable.rehydrate(
                UUID.randomUUID(), "Variable", "Desc", VariableType.NUMERIC,
                peso, true, rangos, List.of());
    }
}
