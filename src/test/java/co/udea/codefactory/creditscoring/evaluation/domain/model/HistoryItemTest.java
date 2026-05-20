package co.udea.codefactory.creditscoring.evaluation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para HistoryItem.
 * Verifica que scoreDelta puede ser null (para la evaluación más antigua)
 * y que los campos obligatorios son no nulos.
 */
class HistoryItemTest {

    @Test
    void historyItem_scoreDeltaPuedeSerNull() {
        HistoryItem item = new HistoryItem(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                BigDecimal.valueOf(75),
                RiskLevel.LOW,
                "Modelo Test",
                1,
                "analista",
                false,
                null // scoreDelta nulo para la evaluación más antigua
        );

        assertThat(item.evaluationId()).isNotNull();
        assertThat(item.evaluatedAt()).isNotNull();
        assertThat(item.totalScore()).isEqualByComparingTo(BigDecimal.valueOf(75));
        assertThat(item.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(item.modelName()).isEqualTo("Modelo Test");
        assertThat(item.modelVersion()).isEqualTo(1);
        assertThat(item.evaluatedBy()).isEqualTo("analista");
        assertThat(item.knockedOut()).isFalse();
        assertThat(item.scoreDelta()).isNull();
    }

    @Test
    void historyItem_scoreDeltaPresente_almacenaValor() {
        BigDecimal delta = BigDecimal.valueOf(10);

        HistoryItem item = new HistoryItem(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                BigDecimal.valueOf(80),
                RiskLevel.VERY_LOW,
                "Modelo 2",
                2,
                "gestor",
                false,
                delta
        );

        assertThat(item.scoreDelta()).isEqualByComparingTo(delta);
    }
}
