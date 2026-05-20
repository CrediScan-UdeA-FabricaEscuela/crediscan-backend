package co.udea.codefactory.creditscoring.evaluation.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.model.search.DecisionFilterValue;

/**
 * Tests unitarios de {@link DecisionFilterValue}.
 */
class DecisionFilterValueTest {

    @Test
    void sinDecision_isSinDecision_retornaTrue() {
        assertThat(DecisionFilterValue.SIN_DECISION.isSinDecision()).isTrue();
    }

    @Test
    void valorReal_isSinDecision_retornaFalse() {
        assertThat(DecisionFilterValue.APPROVED.isSinDecision()).isFalse();
        assertThat(DecisionFilterValue.REJECTED.isSinDecision()).isFalse();
        assertThat(DecisionFilterValue.MANUAL_REVIEW.isSinDecision()).isFalse();
        assertThat(DecisionFilterValue.ESCALATED.isSinDecision()).isFalse();
    }
}
