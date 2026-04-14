package co.udea.codefactory.creditscoring.scoringmodel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.scoringmodel.domain.exception.ScoringModelValidationException;

class ScoringModelTest {

    // =========================================================================
    // Validaciones básicas del agregado
    // =========================================================================

    @Test
    void crear_sinNombre_lanzaExcepcion() {
        assertThatThrownBy(() -> ScoringModel.crear("", "Desc", 1, variablesValidas()))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void crear_conVersionMenorQueUno_lanzaExcepcion() {
        assertThatThrownBy(() -> ScoringModel.crear("v1", "Desc", 0, variablesValidas()))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("versión");
    }

    @Test
    void crear_conDatosValidos_retornaModeloEnDraft() {
        ScoringModel modelo = ScoringModel.crear("Modelo v1", "Desc", 1, variablesValidas());

        assertThat(modelo.id()).isNotNull();
        assertThat(modelo.nombre()).isEqualTo("Modelo v1");
        assertThat(modelo.estado()).isEqualTo(ModelStatus.DRAFT);
        assertThat(modelo.fechaCreacion()).isNotNull();
        assertThat(modelo.fechaActivacion()).isNull();
    }

    // =========================================================================
    // activar() — RN1 y RN2
    // =========================================================================

    @Test
    void activar_conSumaPesosDistintaDeUno_lanzaExcepcion() {
        // Tres variables con peso 0.30 cada una → suma = 0.90 ≠ 1.00 (RN1)
        List<ModelVariable> variables = variablesConPeso(3, new BigDecimal("0.30"));
        ScoringModel modelo = ScoringModel.crear("v1", "Desc", 1, variables);

        assertThatThrownBy(() -> modelo.activar(OffsetDateTime.now(), variables))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("RN1");
    }

    @Test
    void activar_conMenosDeTresVariables_lanzaExcepcion() {
        // Dos variables con suma = 1.00 pero menos de 3 (RN2)
        List<ModelVariable> variables = List.of(
                variableConPeso(new BigDecimal("0.60")),
                variableConPeso(new BigDecimal("0.40")));
        ScoringModel modelo = ScoringModel.crear("v1", "Desc", 1, variables);

        assertThatThrownBy(() -> modelo.activar(OffsetDateTime.now(), variables))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("RN2");
    }

    @Test
    void activar_conModeloNoEnDraft_lanzaExcepcion() {
        List<ModelVariable> variables = variablesValidas();
        ScoringModel draft = ScoringModel.crear("v1", "Desc", 1, variables);
        ScoringModel activo = draft.activar(OffsetDateTime.now(), variables);

        assertThatThrownBy(() -> activo.activar(OffsetDateTime.now(), variables))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("BORRADOR");
    }

    @Test
    void activar_conCondicionesValidas_retornaModeloActivo() {
        List<ModelVariable> variables = variablesValidas();
        ScoringModel draft = ScoringModel.crear("v1", "Desc", 1, variables);
        OffsetDateTime ahora = OffsetDateTime.now();

        ScoringModel activo = draft.activar(ahora, variables);

        assertThat(activo.estado()).isEqualTo(ModelStatus.ACTIVE);
        assertThat(activo.fechaActivacion()).isEqualTo(ahora);
        assertThat(activo.id()).isEqualTo(draft.id());
    }

    // =========================================================================
    // desactivar()
    // =========================================================================

    @Test
    void desactivar_retornaModeloConEstadoInactivo() {
        List<ModelVariable> variables = variablesValidas();
        ScoringModel activo = ScoringModel.crear("v1", "Desc", 1, variables)
                .activar(OffsetDateTime.now(), variables);

        ScoringModel inactivo = activo.desactivar();

        assertThat(inactivo.estado()).isEqualTo(ModelStatus.INACTIVE);
        assertThat(inactivo.id()).isEqualTo(activo.id());
    }

    // =========================================================================
    // esEditable()
    // =========================================================================

    @Test
    void esEditable_enDraft_retornaTrue() {
        ScoringModel draft = ScoringModel.crear("v1", "Desc", 1, variablesValidas());
        assertThat(draft.esEditable()).isTrue();
    }

    @Test
    void esEditable_enActivo_retornaFalse() {
        List<ModelVariable> variables = variablesValidas();
        ScoringModel activo = ScoringModel.crear("v1", "Desc", 1, variables)
                .activar(OffsetDateTime.now(), variables);
        assertThat(activo.esEditable()).isFalse();
    }

    // =========================================================================
    // ModelVariable — validación de peso
    // =========================================================================

    @Test
    void modelVariable_conPesoCero_lanzaExcepcion() {
        assertThatThrownBy(() -> new ModelVariable(
                UUID.randomUUID(), null, UUID.randomUUID(), BigDecimal.ZERO, null))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("peso");
    }

    @Test
    void modelVariable_conPesoMayorQueUno_lanzaExcepcion() {
        assertThatThrownBy(() -> new ModelVariable(
                UUID.randomUUID(), null, UUID.randomUUID(), new BigDecimal("1.01"), null))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("peso");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Tres variables con pesos que suman exactamente 1.00 */
    private List<ModelVariable> variablesValidas() {
        return List.of(
                variableConPeso(new BigDecimal("0.40")),
                variableConPeso(new BigDecimal("0.35")),
                variableConPeso(new BigDecimal("0.25")));
    }

    private List<ModelVariable> variablesConPeso(int cantidad, BigDecimal pesoCada) {
        List<ModelVariable> lista = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            lista.add(variableConPeso(pesoCada));
        }
        return lista;
    }

    private ModelVariable variableConPeso(BigDecimal peso) {
        return new ModelVariable(UUID.randomUUID(), null, UUID.randomUUID(), peso, null);
    }
}
