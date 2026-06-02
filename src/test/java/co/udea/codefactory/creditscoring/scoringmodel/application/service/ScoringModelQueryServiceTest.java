package co.udea.codefactory.creditscoring.scoringmodel.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.ScoringModelComparisonResponse;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelStatus;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ScoringModelQueryServiceTest {

    @Mock
    private ScoringModelRepositoryPort repositorio;

    @InjectMocks
    private ScoringModelQueryService service;

    private static final UUID MODELO_ID = UUID.randomUUID();

    // =========================================================================
    // listar()
    // =========================================================================

    @Test
    void listar_retornaListaDelRepositorio() {
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of());
        when(repositorio.findAll()).thenReturn(List.of(modelo));

        List<ScoringModel> resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(MODELO_ID);
    }

    @Test
    void listar_sinModelos_retornaListaVacia() {
        when(repositorio.findAll()).thenReturn(List.of());

        List<ScoringModel> resultado = service.listar();

        assertThat(resultado).isEmpty();
    }

    // =========================================================================
    // obtener()
    // =========================================================================

    @Test
    void obtener_conIdExistente_retornaModelo() {
        ScoringModel modelo = modeloConVariables(MODELO_ID, List.of());
        when(repositorio.findById(MODELO_ID)).thenReturn(Optional.of(modelo));

        ScoringModel resultado = service.obtener(MODELO_ID);

        assertThat(resultado.id()).isEqualTo(MODELO_ID);
    }

    @Test
    void obtener_conIdInexistente_lanzaResourceNotFoundException() {
        when(repositorio.findById(MODELO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(MODELO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Modelo de scoring");
    }

    // =========================================================================
    // comparar()
    // =========================================================================

    @Test
    void comparar_conBaseInexistente_lanzaResourceNotFoundException() {
        UUID idBase = UUID.randomUUID();
        UUID idComp = UUID.randomUUID();
        when(repositorio.findById(idBase)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comparar(idBase, idComp))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void comparar_conComparadoInexistente_lanzaResourceNotFoundException() {
        UUID idBase = UUID.randomUUID();
        UUID idComp = UUID.randomUUID();
        ScoringModel base = modeloConVariables(idBase, List.of());
        when(repositorio.findById(idBase)).thenReturn(Optional.of(base));
        when(repositorio.findById(idComp)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comparar(idBase, idComp))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void comparar_conVariableEliminadaEnComparado_laReportaComoEliminada() {
        UUID var1Id = UUID.randomUUID();
        UUID idBase = UUID.randomUUID();
        UUID idComp = UUID.randomUUID();

        ModelVariable mvBase = new ModelVariable(UUID.randomUUID(), idBase, var1Id, new BigDecimal("0.5"), null);
        ScoringModel base = modeloConVariables(idBase, List.of(mvBase));
        // comparado sin var1Id → var1 fue eliminada
        ScoringModel comparado = modeloConVariables(idComp, List.of());

        when(repositorio.findById(idBase)).thenReturn(Optional.of(base));
        when(repositorio.findById(idComp)).thenReturn(Optional.of(comparado));

        ScoringModelComparisonResponse resultado = service.comparar(idBase, idComp);

        assertThat(resultado.diferencias())
                .anyMatch(d -> d.variableId().equals(var1Id) && d.tipo().equals("ELIMINADA"));
    }

    @Test
    void comparar_conVariableAgregadaEnComparado_laReportaComoAgregada() {
        UUID var2Id = UUID.randomUUID();
        UUID idBase = UUID.randomUUID();
        UUID idComp = UUID.randomUUID();

        ScoringModel base = modeloConVariables(idBase, List.of());
        ModelVariable mvComp = new ModelVariable(UUID.randomUUID(), idComp, var2Id, new BigDecimal("0.5"), null);
        ScoringModel comparado = modeloConVariables(idComp, List.of(mvComp));

        when(repositorio.findById(idBase)).thenReturn(Optional.of(base));
        when(repositorio.findById(idComp)).thenReturn(Optional.of(comparado));

        ScoringModelComparisonResponse resultado = service.comparar(idBase, idComp);

        assertThat(resultado.diferencias())
                .anyMatch(d -> d.variableId().equals(var2Id) && d.tipo().equals("AGREGADA"));
    }

    @Test
    void comparar_conVariableConPesoDistinto_laReportaComoModificada() {
        UUID varId = UUID.randomUUID();
        UUID idBase = UUID.randomUUID();
        UUID idComp = UUID.randomUUID();

        ModelVariable mvBase = new ModelVariable(UUID.randomUUID(), idBase, varId, new BigDecimal("0.4"), null);
        ModelVariable mvComp = new ModelVariable(UUID.randomUUID(), idComp, varId, new BigDecimal("0.6"), null);
        ScoringModel base = modeloConVariables(idBase, List.of(mvBase));
        ScoringModel comparado = modeloConVariables(idComp, List.of(mvComp));

        when(repositorio.findById(idBase)).thenReturn(Optional.of(base));
        when(repositorio.findById(idComp)).thenReturn(Optional.of(comparado));

        ScoringModelComparisonResponse resultado = service.comparar(idBase, idComp);

        assertThat(resultado.diferencias())
                .anyMatch(d -> d.variableId().equals(varId) && d.tipo().equals("MODIFICADA"));
    }

    @Test
    void comparar_conVariableConMismoPeso_laReportaComoSinCambio() {
        UUID varId = UUID.randomUUID();
        UUID idBase = UUID.randomUUID();
        UUID idComp = UUID.randomUUID();

        ModelVariable mvBase = new ModelVariable(UUID.randomUUID(), idBase, varId, new BigDecimal("0.5"), null);
        ModelVariable mvComp = new ModelVariable(UUID.randomUUID(), idComp, varId, new BigDecimal("0.5"), null);
        ScoringModel base = modeloConVariables(idBase, List.of(mvBase));
        ScoringModel comparado = modeloConVariables(idComp, List.of(mvComp));

        when(repositorio.findById(idBase)).thenReturn(Optional.of(base));
        when(repositorio.findById(idComp)).thenReturn(Optional.of(comparado));

        ScoringModelComparisonResponse resultado = service.comparar(idBase, idComp);

        assertThat(resultado.diferencias())
                .anyMatch(d -> d.variableId().equals(varId) && d.tipo().equals("SIN_CAMBIO"));
    }

    @Test
    void comparar_respuestaContieneAmbosModelos() {
        UUID idBase = UUID.randomUUID();
        UUID idComp = UUID.randomUUID();
        ScoringModel base = modeloConVariables(idBase, List.of());
        ScoringModel comparado = modeloConVariables(idComp, List.of());

        when(repositorio.findById(idBase)).thenReturn(Optional.of(base));
        when(repositorio.findById(idComp)).thenReturn(Optional.of(comparado));

        ScoringModelComparisonResponse resultado = service.comparar(idBase, idComp);

        assertThat(resultado.modeloBase().id()).isEqualTo(idBase);
        assertThat(resultado.modeloComparado().id()).isEqualTo(idComp);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ScoringModel modeloConVariables(UUID id, List<ModelVariable> variables) {
        return ScoringModel.rehydrate(id, "Modelo Test", "Descripción", 1, ModelStatus.DRAFT,
                variables, OffsetDateTime.now(), null);
    }
}
