package co.udea.codefactory.creditscoring.reporting.application.util;

/**
 * Categorías de concordancia entre el nivel de riesgo del modelo y la decisión del analista.
 * <p>
 * - CONCORDANT: el modelo y el analista coinciden en la valoración del riesgo.<br>
 * - OVERRIDE_APPROVE_HIGH_RISK: el modelo predijo riesgo alto pero el analista aprobó.<br>
 * - OVERRIDE_REJECT_LOW_RISK: el modelo predijo riesgo bajo pero el analista rechazó.<br>
 * - NEUTRAL_FOR_MEDIUM: nivel MEDIUM — no se puede clasificar como concordante ni como override.<br>
 * - NOT_APPLICABLE: la decisión es MANUAL_REVIEW o ESCALATED — no participa en el cálculo de tasas.
 */
public enum ConcordanceCategory {
    CONCORDANT,
    OVERRIDE_APPROVE_HIGH_RISK,
    OVERRIDE_REJECT_LOW_RISK,
    NEUTRAL_FOR_MEDIUM,
    NOT_APPLICABLE
}
