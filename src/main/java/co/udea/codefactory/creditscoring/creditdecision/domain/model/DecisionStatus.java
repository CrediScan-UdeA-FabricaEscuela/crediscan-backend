package co.udea.codefactory.creditscoring.creditdecision.domain.model;

/**
 * Enumeración de los estados posibles de una decisión crediticia.
 *
 * <p>Cada estado representa una decisión final sobre una evaluación:
 * Aprobada, Rechazada, En Revisión (Manual Review) o Escalada
 * (requiere aprobación de nivel superior).</p>
 */
public enum DecisionStatus {
    APPROVED,
    REJECTED,
    MANUAL_REVIEW,
    ESCALATED
}
