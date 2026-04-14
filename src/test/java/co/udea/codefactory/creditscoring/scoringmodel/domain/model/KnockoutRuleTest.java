package co.udea.codefactory.creditscoring.scoringmodel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.scoringmodel.domain.exception.ScoringModelValidationException;

class KnockoutRuleTest {

    private static final UUID MODELO_ID = UUID.randomUUID();

    // =========================================================================
    // Validaciones del constructor
    // =========================================================================

    @Test
    void crear_sinModeloId_lanzaExcepcion() {
        assertThatThrownBy(() -> KnockoutRule.crear(
                null, "moras_12_meses", KnockoutOperator.GT, BigDecimal.valueOf(3),
                "Más de 3 moras", 1))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("modelo");
    }

    @Test
    void crear_sinCampo_lanzaExcepcion() {
        assertThatThrownBy(() -> KnockoutRule.crear(
                MODELO_ID, "", KnockoutOperator.GT, BigDecimal.valueOf(3),
                "Más de 3 moras", 1))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("campo");
    }

    @Test
    void crear_sinOperador_lanzaExcepcion() {
        assertThatThrownBy(() -> KnockoutRule.crear(
                MODELO_ID, "moras_12_meses", null, BigDecimal.valueOf(3),
                "Más de 3 moras", 1))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("operador");
    }

    @Test
    void crear_sinUmbral_lanzaExcepcion() {
        assertThatThrownBy(() -> KnockoutRule.crear(
                MODELO_ID, "moras_12_meses", KnockoutOperator.GT, null,
                "Más de 3 moras", 1))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("umbral");
    }

    @Test
    void crear_conPrioridadNegativa_lanzaExcepcion() {
        assertThatThrownBy(() -> KnockoutRule.crear(
                MODELO_ID, "moras_12_meses", KnockoutOperator.GT, BigDecimal.valueOf(3),
                "Más de 3 moras", -1))
                .isInstanceOf(ScoringModelValidationException.class)
                .hasMessageContaining("prioridad");
    }

    @Test
    void crear_conDatosValidos_creaReglaActiva() {
        KnockoutRule regla = KnockoutRule.crear(
                MODELO_ID, "moras_12_meses", KnockoutOperator.GT, BigDecimal.valueOf(3),
                "Más de 3 moras en los últimos 12 meses", 1);

        assertThat(regla.id()).isNotNull();
        assertThat(regla.modeloId()).isEqualTo(MODELO_ID);
        assertThat(regla.operador()).isEqualTo(KnockoutOperator.GT);
        assertThat(regla.activa()).isTrue();
    }

    // =========================================================================
    // KnockoutOperator — evaluación
    // =========================================================================

    @Test
    void gt_activaCuandoValorEsMayorQueUmbral() {
        KnockoutRule regla = reglaConOperador(KnockoutOperator.GT, BigDecimal.valueOf(3));
        assertThat(regla.evaluar(BigDecimal.valueOf(4))).isTrue();
        assertThat(regla.evaluar(BigDecimal.valueOf(3))).isFalse();
        assertThat(regla.evaluar(BigDecimal.valueOf(2))).isFalse();
    }

    @Test
    void lt_activaCuandoValorEsMenorQueUmbral() {
        KnockoutRule regla = reglaConOperador(KnockoutOperator.LT, BigDecimal.valueOf(500));
        assertThat(regla.evaluar(BigDecimal.valueOf(499))).isTrue();
        assertThat(regla.evaluar(BigDecimal.valueOf(500))).isFalse();
    }

    @Test
    void gte_activaCuandoValorEsMayorOIgualQueUmbral() {
        KnockoutRule regla = reglaConOperador(KnockoutOperator.GTE, BigDecimal.valueOf(3));
        assertThat(regla.evaluar(BigDecimal.valueOf(3))).isTrue();
        assertThat(regla.evaluar(BigDecimal.valueOf(4))).isTrue();
        assertThat(regla.evaluar(BigDecimal.valueOf(2))).isFalse();
    }

    @Test
    void lte_activaCuandoValorEsMenorOIgualQueUmbral() {
        KnockoutRule regla = reglaConOperador(KnockoutOperator.LTE, BigDecimal.valueOf(0));
        assertThat(regla.evaluar(BigDecimal.ZERO)).isTrue();
        assertThat(regla.evaluar(BigDecimal.valueOf(-1))).isTrue();
        assertThat(regla.evaluar(BigDecimal.valueOf(1))).isFalse();
    }

    @Test
    void eq_activaSoloCuandoValorEsIgualAlUmbral() {
        KnockoutRule regla = reglaConOperador(KnockoutOperator.EQ, BigDecimal.valueOf(0));
        assertThat(regla.evaluar(BigDecimal.ZERO)).isTrue();
        assertThat(regla.evaluar(BigDecimal.ONE)).isFalse();
    }

    @Test
    void neq_activaCuandoValorEsDistintoDelUmbral() {
        KnockoutRule regla = reglaConOperador(KnockoutOperator.NEQ, BigDecimal.valueOf(0));
        assertThat(regla.evaluar(BigDecimal.ONE)).isTrue();
        assertThat(regla.evaluar(BigDecimal.ZERO)).isFalse();
    }

    @Test
    void evaluar_reglaDesactivada_siempreRetornaFalse() {
        KnockoutRule activa = reglaConOperador(KnockoutOperator.GT, BigDecimal.valueOf(3));
        KnockoutRule inactiva = activa.desactivar();

        // El valor 100 activaría la regla si estuviera activa
        assertThat(inactiva.evaluar(BigDecimal.valueOf(100))).isFalse();
        assertThat(inactiva.activa()).isFalse();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private KnockoutRule reglaConOperador(KnockoutOperator operador, BigDecimal umbral) {
        return KnockoutRule.crear(
                MODELO_ID, "campo_test", operador, umbral, "Mensaje de prueba", 0);
    }
}
