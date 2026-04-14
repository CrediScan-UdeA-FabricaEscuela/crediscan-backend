package co.udea.codefactory.creditscoring.scoring.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.scoring.domain.exception.ScoringVariableValidationException;

class ScoringVariableTest {

    // =========================================================================
    // Validaciones básicas del agregado
    // =========================================================================

    @Test
    void crear_sinNombre_lanzaExcepcion() {
        assertThatThrownBy(() -> ScoringVariable.crear(
                "", "Descripción", VariableType.NUMERIC, new BigDecimal("0.30"),
                rangoValido(), List.of()))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void crear_sinTipo_lanzaExcepcion() {
        assertThatThrownBy(() -> ScoringVariable.crear(
                "Mi variable", "Desc", null, new BigDecimal("0.30"),
                rangoValido(), List.of()))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("tipo");
    }

    @Test
    void crear_pesoMenorQueMinimo_lanzaExcepcion() {
        assertThatThrownBy(() -> ScoringVariable.crear(
                "Mi variable", "Desc", VariableType.NUMERIC, new BigDecimal("0.00"),
                rangoValido(), List.of()))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("peso");
    }

    @Test
    void crear_pesoMayorQueUno_lanzaExcepcion() {
        assertThatThrownBy(() -> ScoringVariable.crear(
                "Mi variable", "Desc", VariableType.NUMERIC, new BigDecimal("1.01"),
                rangoValido(), List.of()))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("peso");
    }

    @Test
    void crear_variableNumerica_conDatosValidos_retornaVariable() {
        ScoringVariable variable = ScoringVariable.crear(
                "Antigüedad laboral", "Años en el empleo actual",
                VariableType.NUMERIC, new BigDecimal("0.30"),
                rangoValido(), List.of());

        assertThat(variable.id()).isNotNull();
        assertThat(variable.nombre()).isEqualTo("Antigüedad laboral");
        assertThat(variable.tipo()).isEqualTo(VariableType.NUMERIC);
        assertThat(variable.activa()).isTrue();
    }

    // =========================================================================
    // CA5 / RN5: Validación de rangos numéricos
    // =========================================================================

    @Test
    void crear_variableNumerica_sinRangos_lanzaExcepcion() {
        assertThatThrownBy(() -> ScoringVariable.crear(
                "Antigüedad", "Desc", VariableType.NUMERIC, new BigDecimal("0.20"),
                List.of(), List.of()))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("al menos un rango");
    }

    @Test
    void crear_variableNumerica_primerRangoNoEmpienzaEn0_lanzaExcepcion() {
        UUID id = UUID.randomUUID();
        List<VariableRange> rangos = List.of(
                new VariableRange(id, null, new BigDecimal("5"), new BigDecimal("10"), 50, null));

        assertThatThrownBy(() -> ScoringVariable.crear(
                "Variable", "Desc", VariableType.NUMERIC, new BigDecimal("0.20"),
                rangos, List.of()))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("0");
    }

    @Test
    void crear_variableNumerica_conGapEntreRangos_lanzaExcepcion() {
        List<VariableRange> rangos = List.of(
                new VariableRange(UUID.randomUUID(), null, BigDecimal.ZERO, new BigDecimal("5"), 30, null),
                new VariableRange(UUID.randomUUID(), null, new BigDecimal("6"), new BigDecimal("10"), 60, null));

        assertThatThrownBy(() -> ScoringVariable.crear(
                "Variable", "Desc", VariableType.NUMERIC, new BigDecimal("0.20"),
                rangos, List.of()))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("contiguos");
    }

    @Test
    void crear_variableNumerica_conRangosContiguos_esValido() {
        List<VariableRange> rangos = List.of(
                new VariableRange(UUID.randomUUID(), null, BigDecimal.ZERO, new BigDecimal("5"), 30, "Bajo"),
                new VariableRange(UUID.randomUUID(), null, new BigDecimal("5"), new BigDecimal("10"), 70, "Alto"));

        ScoringVariable variable = ScoringVariable.crear(
                "Antigüedad", "Desc", VariableType.NUMERIC, new BigDecimal("0.20"),
                rangos, List.of());

        assertThat(variable.rangos()).hasSize(2);
    }

    // =========================================================================
    // CA6: Validación de categorías
    // =========================================================================

    @Test
    void crear_variableCategorica_sinCategorias_lanzaExcepcion() {
        assertThatThrownBy(() -> ScoringVariable.crear(
                "Tipo empleo", "Desc", VariableType.CATEGORICAL, new BigDecimal("0.20"),
                List.of(), List.of()))
                .isInstanceOf(ScoringVariableValidationException.class)
                .hasMessageContaining("al menos una categoría");
    }

    @Test
    void crear_variableCategorica_conCategorias_esValido() {
        List<VariableCategory> categorias = List.of(
                new VariableCategory(UUID.randomUUID(), null, "Empleado", 80, null),
                new VariableCategory(UUID.randomUUID(), null, "Independiente", 50, null));

        ScoringVariable variable = ScoringVariable.crear(
                "Tipo empleo", "Desc", VariableType.CATEGORICAL, new BigDecimal("0.20"),
                List.of(), categorias);

        assertThat(variable.categorias()).hasSize(2);
    }

    // =========================================================================
    // Mutaciones
    // =========================================================================

    @Test
    void desactivar_devuelveNuevaInstanciaConActivaFalse() {
        ScoringVariable activa = ScoringVariable.crear(
                "Variable", "Desc", VariableType.NUMERIC, new BigDecimal("0.30"),
                rangoValido(), List.of());

        ScoringVariable inactiva = activa.desactivar();

        assertThat(activa.activa()).isTrue();
        assertThat(inactiva.activa()).isFalse();
        assertThat(inactiva.id()).isEqualTo(activa.id());
    }

    @Test
    void activar_devuelveNuevaInstanciaConActivaTrue() {
        ScoringVariable activa = ScoringVariable.crear(
                "Variable", "Desc", VariableType.NUMERIC, new BigDecimal("0.30"),
                rangoValido(), List.of());
        ScoringVariable inactiva = activa.desactivar();

        ScoringVariable reactivada = inactiva.activar();

        assertThat(reactivada.activa()).isTrue();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<VariableRange> rangoValido() {
        return List.of(
                new VariableRange(UUID.randomUUID(), null, BigDecimal.ZERO, new BigDecimal("100"), 50, "Rango único"));
    }
}
