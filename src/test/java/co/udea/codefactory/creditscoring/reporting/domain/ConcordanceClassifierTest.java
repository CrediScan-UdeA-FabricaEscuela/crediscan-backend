package co.udea.codefactory.creditscoring.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.DecisionStatus;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.reporting.application.util.ConcordanceCategory;
import co.udea.codefactory.creditscoring.reporting.application.util.ConcordanceClassifier;

/**
 * Tests unitarios para ConcordanceClassifier.
 * Verifica todas las reglas de concordancia, incluyendo REJECTED=VERY_HIGH (decisión #5).
 * Sin Spring — JUnit 5 puro.
 */
class ConcordanceClassifierTest {

    // =========================================================================
    // Concordantes — nivel bajo + APPROVED
    // =========================================================================

    @Test
    void veryLow_approved_esConcordante() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.VERY_LOW, DecisionStatus.APPROVED))
                .isEqualTo(ConcordanceCategory.CONCORDANT);
    }

    @Test
    void low_approved_esConcordante() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.LOW, DecisionStatus.APPROVED))
                .isEqualTo(ConcordanceCategory.CONCORDANT);
    }

    // =========================================================================
    // Concordantes — nivel alto + REJECTED
    // =========================================================================

    @Test
    void high_rejected_esConcordante() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.HIGH, DecisionStatus.REJECTED))
                .isEqualTo(ConcordanceCategory.CONCORDANT);
    }

    @Test
    void veryHigh_rejected_esConcordante() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.VERY_HIGH, DecisionStatus.REJECTED))
                .isEqualTo(ConcordanceCategory.CONCORDANT);
    }

    // =========================================================================
    // REJECTED RiskLevel tratado como VERY_HIGH (decisión #5)
    // =========================================================================

    @Test
    void riskLevelRejected_decisionRejected_esConcordante() {
        // REJECTED → VERY_HIGH → VERY_HIGH + REJECTED = CONCORDANT
        assertThat(ConcordanceClassifier.classify(RiskLevel.REJECTED, DecisionStatus.REJECTED))
                .isEqualTo(ConcordanceCategory.CONCORDANT);
    }

    @Test
    void riskLevelRejected_decisionApproved_esOverrideAprobarRiesgoAlto() {
        // REJECTED → VERY_HIGH → VERY_HIGH + APPROVED = OVERRIDE_APPROVE_HIGH_RISK
        assertThat(ConcordanceClassifier.classify(RiskLevel.REJECTED, DecisionStatus.APPROVED))
                .isEqualTo(ConcordanceCategory.OVERRIDE_APPROVE_HIGH_RISK);
    }

    // =========================================================================
    // Overrides — alto riesgo aprobado
    // =========================================================================

    @Test
    void high_approved_esOverrideAprobarRiesgoAlto() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.HIGH, DecisionStatus.APPROVED))
                .isEqualTo(ConcordanceCategory.OVERRIDE_APPROVE_HIGH_RISK);
    }

    @Test
    void veryHigh_approved_esOverrideAprobarRiesgoAlto() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.VERY_HIGH, DecisionStatus.APPROVED))
                .isEqualTo(ConcordanceCategory.OVERRIDE_APPROVE_HIGH_RISK);
    }

    // =========================================================================
    // Overrides — bajo riesgo rechazado
    // =========================================================================

    @Test
    void veryLow_rejected_esOverrideRechazarRiesgoBajo() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.VERY_LOW, DecisionStatus.REJECTED))
                .isEqualTo(ConcordanceCategory.OVERRIDE_REJECT_LOW_RISK);
    }

    @Test
    void low_rejected_esOverrideRechazarRiesgoBajo() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.LOW, DecisionStatus.REJECTED))
                .isEqualTo(ConcordanceCategory.OVERRIDE_REJECT_LOW_RISK);
    }

    // =========================================================================
    // MEDIUM — neutral
    // =========================================================================

    @Test
    void medium_approved_esNeutral() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.MEDIUM, DecisionStatus.APPROVED))
                .isEqualTo(ConcordanceCategory.NEUTRAL_FOR_MEDIUM);
    }

    @Test
    void medium_rejected_esNeutral() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.MEDIUM, DecisionStatus.REJECTED))
                .isEqualTo(ConcordanceCategory.NEUTRAL_FOR_MEDIUM);
    }

    // =========================================================================
    // MANUAL_REVIEW y ESCALATED — not applicable
    // =========================================================================

    @Test
    void cualquierNivel_manualReview_esNotApplicable() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.HIGH, DecisionStatus.MANUAL_REVIEW))
                .isEqualTo(ConcordanceCategory.NOT_APPLICABLE);
    }

    @Test
    void cualquierNivel_escalated_esNotApplicable() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.LOW, DecisionStatus.ESCALATED))
                .isEqualTo(ConcordanceCategory.NOT_APPLICABLE);
    }

    @Test
    void medium_manualReview_esNotApplicable() {
        assertThat(ConcordanceClassifier.classify(RiskLevel.MEDIUM, DecisionStatus.MANUAL_REVIEW))
                .isEqualTo(ConcordanceCategory.NOT_APPLICABLE);
    }

    // =========================================================================
    // isConcordant helper
    // =========================================================================

    @Test
    void concordant_isConcordantTrue() {
        assertThat(ConcordanceClassifier.isConcordant(RiskLevel.VERY_LOW, DecisionStatus.APPROVED))
                .isTrue();
    }

    @Test
    void override_isConcordantFalse() {
        assertThat(ConcordanceClassifier.isConcordant(RiskLevel.HIGH, DecisionStatus.APPROVED))
                .isFalse();
    }

    @Test
    void notApplicable_isConcordantFalse() {
        assertThat(ConcordanceClassifier.isConcordant(RiskLevel.HIGH, DecisionStatus.MANUAL_REVIEW))
                .isFalse();
    }
}
