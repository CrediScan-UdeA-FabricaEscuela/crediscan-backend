package co.udea.codefactory.creditscoring.evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetail;

/**
 * Tests unitarios para el record EvaluationDetail.
 * Verifica factories y validaciones del compact constructor.
 */
class EvaluationDetailTest {

    private final UUID varId = UUID.randomUUID();

    @Test
    void crear_camposValidos_retornaDetailConIdGenerado() {
        EvaluationDetail detail = EvaluationDetail.crear(
                varId, "moras_12_meses", "5",
                BigDecimal.valueOf(70), BigDecimal.valueOf(0.40), BigDecimal.valueOf(28));

        assertThat(detail.id()).isNotNull();
        assertThat(detail.variableId()).isEqualTo(varId);
        assertThat(detail.variableName()).isEqualTo("moras_12_meses");
        assertThat(detail.rawValue()).isEqualTo("5");
        assertThat(detail.createdAt()).isNotNull();
    }

    @Test
    void rehydrate_todosLosCampos_reconstruyeCorrectamente() {
        UUID id = UUID.randomUUID();
        OffsetDateTime ts = OffsetDateTime.now();
        EvaluationDetail detail = EvaluationDetail.rehydrate(
                id, varId, "score_buro", "720",
                BigDecimal.valueOf(70), BigDecimal.valueOf(0.35), BigDecimal.valueOf(24.5), ts);

        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.createdAt()).isEqualTo(ts);
    }

    @Test
    void compact_idNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new EvaluationDetail(
                null, varId, "nombre", "val",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void compact_variableIdNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new EvaluationDetail(
                UUID.randomUUID(), null, "nombre", "val",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("variableId");
    }

    @Test
    void compact_variableNameBlank_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new EvaluationDetail(
                UUID.randomUUID(), varId, "  ", "val",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, OffsetDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");
    }
}
