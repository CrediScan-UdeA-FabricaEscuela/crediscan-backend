package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence.analistas;

/**
 * Proyección para la query de conteos de decisiones por analista (HU-017).
 */
public interface AnalistaCountsProjection {
    /** Identificador del analista ({@code evaluation.evaluated_by}). */
    String getEvaluatedBy();

    /** Total de evaluaciones con decisión. */
    long getTotal();

    /** Cantidad de decisiones APPROVED. */
    long getAprobadas();

    /** Cantidad de decisiones REJECTED. */
    long getRechazadas();

    /** Cantidad de decisiones MANUAL_REVIEW. */
    long getRevisionManual();

    /** Cantidad de decisiones ESCALATED. */
    long getEscaladas();
}
