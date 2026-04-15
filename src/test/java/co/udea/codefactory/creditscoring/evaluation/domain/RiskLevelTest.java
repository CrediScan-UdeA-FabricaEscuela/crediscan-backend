package co.udea.codefactory.creditscoring.evaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Tests unitarios para la clasificación de riesgo por puntaje.
 * Verifica todos los boundary values del enum RiskLevel.
 */
class RiskLevelTest {

    // =========================================================================
    // Boundary values — clasificación por puntaje
    // =========================================================================

    @ParameterizedTest(name = "score={0} -> {1}")
    @CsvSource({
        "0,   VERY_HIGH",
        "29,  VERY_HIGH",
        "30,  HIGH",
        "49,  HIGH",
        "50,  MEDIUM",
        "69,  MEDIUM",
        "70,  LOW",
        "84,  LOW",
        "85,  VERY_LOW",
        "100, VERY_LOW"
    })
    void fromScore_boundaryValues_clasificaCorrectamente(int score, RiskLevel expected) {
        RiskLevel result = RiskLevel.fromScore(BigDecimal.valueOf(score));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void fromScore_scoreNull_lanzaIllegalArgument() {
        assertThatThrownBy(() -> RiskLevel.fromScore(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    void fromScore_scoreMayorA100_lanzaIllegalArgument() {
        assertThatThrownBy(() -> RiskLevel.fromScore(BigDecimal.valueOf(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100");
    }

    @Test
    void fromScore_scoreNegativo_lanzaIllegalArgument() {
        assertThatThrownBy(() -> RiskLevel.fromScore(BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // REJECTED — nivel especial para knockouts
    // =========================================================================

    @Test
    void rejected_retornaREJECTED() {
        assertThat(RiskLevel.rejected()).isEqualTo(RiskLevel.REJECTED);
    }

    @Test
    void rejected_noEsAccesibleDesdeFromScore() {
        // REJECTED no debe aparecer en fromScore con ningún puntaje válido
        for (int i = 0; i <= 100; i++) {
            assertThat(RiskLevel.fromScore(BigDecimal.valueOf(i)))
                    .isNotEqualTo(RiskLevel.REJECTED);
        }
    }

    // =========================================================================
    // Getters de rango
    // =========================================================================

    @Test
    void getMinScore_veryLow_retorna85() {
        assertThat(RiskLevel.VERY_LOW.getMinScore()).isEqualTo(85);
    }

    @Test
    void getMaxScore_veryLow_retorna100() {
        assertThat(RiskLevel.VERY_LOW.getMaxScore()).isEqualTo(100);
    }

    @Test
    void getters_rejected_retornaMinusUno() {
        assertThat(RiskLevel.REJECTED.getMinScore()).isEqualTo(-1);
        assertThat(RiskLevel.REJECTED.getMaxScore()).isEqualTo(-1);
    }
}
