package co.udea.codefactory.creditscoring.evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationKnockout;

/**
 * Tests unitarios para el record EvaluationKnockout.
 * Verifica factories y validaciones del compact constructor.
 */
class EvaluationKnockoutTest {

    private final UUID ruleId = UUID.randomUUID();

    @Test
    void crear_camposValidos_retornaKnockoutConIdGenerado() {
        EvaluationKnockout ko = EvaluationKnockout.crear(ruleId, "moras_12_meses", "5", true);

        assertThat(ko.id()).isNotNull();
        assertThat(ko.ruleId()).isEqualTo(ruleId);
        assertThat(ko.ruleName()).isEqualTo("moras_12_meses");
        assertThat(ko.fieldValue()).isEqualTo("5");
        assertThat(ko.triggered()).isTrue();
        assertThat(ko.createdAt()).isNotNull();
    }

    @Test
    void rehydrate_todosLosCampos_reconstruyeCorrectamente() {
        UUID id = UUID.randomUUID();
        OffsetDateTime ts = OffsetDateTime.now();
        EvaluationKnockout ko = EvaluationKnockout.rehydrate(
                id, ruleId, "deudas", "0", false, ts);

        assertThat(ko.id()).isEqualTo(id);
        assertThat(ko.triggered()).isFalse();
        assertThat(ko.createdAt()).isEqualTo(ts);
    }

    @Test
    void compact_idNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new EvaluationKnockout(
                null, ruleId, "nombre", "val", false, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void compact_ruleIdNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new EvaluationKnockout(
                UUID.randomUUID(), null, "nombre", "val", false, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruleId");
    }

    @Test
    void compact_ruleNameBlank_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new EvaluationKnockout(
                UUID.randomUUID(), ruleId, "", "val", false, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void compact_fieldValueNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new EvaluationKnockout(
                UUID.randomUUID(), ruleId, "nombre", null, false, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fieldValue");
    }
}
