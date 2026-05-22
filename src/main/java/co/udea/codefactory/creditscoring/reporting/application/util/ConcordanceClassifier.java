package co.udea.codefactory.creditscoring.reporting.application.util;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.DecisionStatus;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Clasificador de concordancia entre el nivel de riesgo del modelo automático
 * y la decisión del analista humano.
 *
 * <p>Decisión de diseño bloqueada #5: {@code RiskLevel.REJECTED} se trata como
 * {@code RiskLevel.VERY_HIGH} en todos los cálculos de concordancia.</p>
 *
 * <p>El denominador para las tasas de override solo incluye APPROVED y REJECTED;
 * MANUAL_REVIEW y ESCALATED se clasifican como NOT_APPLICABLE y quedan fuera del cálculo.</p>
 */
public final class ConcordanceClassifier {

    private ConcordanceClassifier() {
        // utilidad estática — no instanciar
    }

    /**
     * Clasifica la relación entre el nivel de riesgo y la decisión del analista.
     *
     * @param riskLevel nivel de riesgo del modelo automático
     * @param decision  decisión del analista
     * @return categoría de concordancia correspondiente
     */
    public static ConcordanceCategory classify(RiskLevel riskLevel, DecisionStatus decision) {
        // Decisiones que no participan en el cálculo de concordancia
        if (decision == DecisionStatus.MANUAL_REVIEW || decision == DecisionStatus.ESCALATED) {
            return ConcordanceCategory.NOT_APPLICABLE;
        }

        // REJECTED se trata como VERY_HIGH (decisión bloqueada #5)
        RiskLevel efectivo = effectiveLevel(riskLevel);

        return switch (efectivo) {
            case VERY_LOW, LOW -> decision == DecisionStatus.APPROVED
                    ? ConcordanceCategory.CONCORDANT
                    : ConcordanceCategory.OVERRIDE_REJECT_LOW_RISK;

            case MEDIUM -> ConcordanceCategory.NEUTRAL_FOR_MEDIUM;

            case HIGH, VERY_HIGH -> decision == DecisionStatus.REJECTED
                    ? ConcordanceCategory.CONCORDANT
                    : ConcordanceCategory.OVERRIDE_APPROVE_HIGH_RISK;

            // REJECTED ya fue mapeado a VERY_HIGH — este caso no puede ocurrir
            default -> ConcordanceCategory.NOT_APPLICABLE;
        };
    }

    /**
     * Devuelve {@code true} si la combinación es concordante.
     * Solo CONCORDANT retorna true; NOT_APPLICABLE, NEUTRAL y overrides retornan false.
     */
    public static boolean isConcordant(RiskLevel riskLevel, DecisionStatus decision) {
        return classify(riskLevel, decision) == ConcordanceCategory.CONCORDANT;
    }

    /**
     * Devuelve el nivel efectivo para cálculos de concordancia.
     * REJECTED se trata como VERY_HIGH (decisión bloqueada #5).
     */
    public static RiskLevel effectiveLevel(RiskLevel riskLevel) {
        return riskLevel == RiskLevel.REJECTED ? RiskLevel.VERY_HIGH : riskLevel;
    }
}
