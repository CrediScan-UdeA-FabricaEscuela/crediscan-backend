package co.udea.codefactory.creditscoring.evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Tests unitarios para el agregado raíz Evaluation.
 * Verifica factories, validaciones del compact constructor e invariantes del dominio.
 */
class EvaluationTest {

    private final UUID applicantId = UUID.randomUUID();
    private final UUID modelId = UUID.randomUUID();
    private final UUID financialDataId = UUID.randomUUID();

    @Test
    void crear_camposValidos_retornaEvaluacionConIdGenerado() {
        Evaluation ev = Evaluation.crear(
                applicantId, modelId, financialDataId,
                BigDecimal.valueOf(75), RiskLevel.LOW,
                false, null, "analista", List.of(), List.of());

        assertThat(ev.id()).isNotNull();
        assertThat(ev.applicantId()).isEqualTo(applicantId);
        assertThat(ev.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(ev.knockedOut()).isFalse();
        assertThat(ev.evaluatedBy()).isEqualTo("analista");
        assertThat(ev.createdBy()).isEqualTo("analista");
        assertThat(ev.evaluatedAt()).isNotNull();
        assertThat(ev.createdAt()).isNotNull();
        assertThat(ev.details()).isEmpty();
        assertThat(ev.knockouts()).isEmpty();
    }

    @Test
    void crear_koRechazado_totalScoreEsCero() {
        // Cuando hay KO el puntaje es ZERO — el compact constructor lo acepta
        Evaluation ev = Evaluation.crear(
                applicantId, modelId, financialDataId,
                BigDecimal.ZERO, RiskLevel.REJECTED,
                true, "Más de 3 moras", "analista", List.of(), List.of());

        assertThat(ev.knockedOut()).isTrue();
        assertThat(ev.riskLevel()).isEqualTo(RiskLevel.REJECTED);
        assertThat(ev.totalScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rehydrate_todosLosCampos_reconstruyeCorrectamente() {
        UUID id = UUID.randomUUID();
        OffsetDateTime ts = OffsetDateTime.now();
        Evaluation ev = Evaluation.rehydrate(
                id, applicantId, modelId, financialDataId,
                BigDecimal.valueOf(55), RiskLevel.MEDIUM, false, null,
                ts, "analista", ts, "analista", List.of(), List.of());

        assertThat(ev.id()).isEqualTo(id);
        assertThat(ev.evaluatedAt()).isEqualTo(ts);
    }

    @Test
    void compact_idNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new Evaluation(
                null, applicantId, modelId, financialDataId,
                BigDecimal.valueOf(50), RiskLevel.MEDIUM,
                false, null, OffsetDateTime.now(), "u",
                OffsetDateTime.now(), "u", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void compact_applicantIdNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new Evaluation(
                UUID.randomUUID(), null, modelId, financialDataId,
                BigDecimal.valueOf(50), RiskLevel.MEDIUM,
                false, null, OffsetDateTime.now(), "u",
                OffsetDateTime.now(), "u", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicantId");
    }

    @Test
    void compact_modelIdNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new Evaluation(
                UUID.randomUUID(), applicantId, null, financialDataId,
                BigDecimal.valueOf(50), RiskLevel.MEDIUM,
                false, null, OffsetDateTime.now(), "u",
                OffsetDateTime.now(), "u", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modelId");
    }

    @Test
    void compact_financialDataIdNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new Evaluation(
                UUID.randomUUID(), applicantId, modelId, null,
                BigDecimal.valueOf(50), RiskLevel.MEDIUM,
                false, null, OffsetDateTime.now(), "u",
                OffsetDateTime.now(), "u", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("financialDataId");
    }

    @Test
    void compact_totalScoreNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new Evaluation(
                UUID.randomUUID(), applicantId, modelId, financialDataId,
                null, RiskLevel.MEDIUM,
                false, null, OffsetDateTime.now(), "u",
                OffsetDateTime.now(), "u", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("puntaje");
    }

    @Test
    void compact_totalScoreMayorA100_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new Evaluation(
                UUID.randomUUID(), applicantId, modelId, financialDataId,
                BigDecimal.valueOf(101), RiskLevel.MEDIUM,
                false, null, OffsetDateTime.now(), "u",
                OffsetDateTime.now(), "u", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("puntaje");
    }

    @Test
    void compact_riskLevelNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new Evaluation(
                UUID.randomUUID(), applicantId, modelId, financialDataId,
                BigDecimal.valueOf(50), null,
                false, null, OffsetDateTime.now(), "u",
                OffsetDateTime.now(), "u", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riesgo");
    }

    @Test
    void compact_evaluatedByBlank_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new Evaluation(
                UUID.randomUUID(), applicantId, modelId, financialDataId,
                BigDecimal.valueOf(50), RiskLevel.MEDIUM,
                false, null, OffsetDateTime.now(), "  ",
                OffsetDateTime.now(), "u", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evaluador");
    }

    @Test
    void compact_createdAtNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> new Evaluation(
                UUID.randomUUID(), applicantId, modelId, financialDataId,
                BigDecimal.valueOf(50), RiskLevel.MEDIUM,
                false, null, OffsetDateTime.now(), "u",
                null, "u", List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creacion");
    }

    @Test
    void compact_listasNull_seConviertenEnListasVacias() {
        Evaluation ev = Evaluation.crear(
                applicantId, modelId, financialDataId,
                BigDecimal.valueOf(50), RiskLevel.MEDIUM,
                false, null, "analista", null, null);

        assertThat(ev.details()).isEmpty();
        assertThat(ev.knockouts()).isEmpty();
    }
}
