package co.udea.codefactory.creditscoring.creditdecision.domain.model;

/**
 * Enumeración de los estados posibles de una decisión crediticia.
 *
 * <p>Cada estado representa una decisión final sobre una evaluación:
 * Aprobada, Rechazada, En Revisión (Manual Review) o Escalada
 * (requiere aprobación de nivel superior).</p>
 */
public enum DecisionStatus {
    APPROVED("Aprobado"),
    REJECTED("Rechazado"),
    MANUAL_REVIEW("Revisión Manual"),
    ESCALATED("Escalado");

    private final String etiqueta;

    DecisionStatus(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Etiqueta en español para mostrar en reportes y PDFs generados. */
    public String getEtiqueta() { return etiqueta; }
}
