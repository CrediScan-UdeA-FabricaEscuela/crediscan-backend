package co.udea.codefactory.creditscoring.evaluation.domain.model.search;

/**
 * Valores de filtro para el campo {@code decision} en la búsqueda avanzada de evaluaciones.
 *
 * <p>Los valores reales ({@code APPROVED}, {@code REJECTED}, etc.) coinciden 1:1 con
 * {@code creditdecision.DecisionStatus}. {@code SIN_DECISION} es sintético y se traduce
 * en la capa de persistencia a {@code cd.decision IS NULL}.</p>
 */
public enum DecisionFilterValue {

    APPROVED,
    REJECTED,
    MANUAL_REVIEW,
    ESCALATED,
    SIN_DECISION;

    /** Retorna {@code true} si este valor representa ausencia de decisión en BD. */
    public boolean isSinDecision() {
        return this == SIN_DECISION;
    }
}
