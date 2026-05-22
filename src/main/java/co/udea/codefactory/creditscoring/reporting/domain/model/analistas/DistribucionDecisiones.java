package co.udea.codefactory.creditscoring.reporting.domain.model.analistas;

import java.math.BigDecimal;

/**
 * Distribución de decisiones de un analista con porcentajes.
 * <p>
 * La suma de los cuatro porcentajes es exactamente 100.00.
 * El denominador incluye todas las decisiones (APPROVED, REJECTED, MANUAL_REVIEW, ESCALATED).
 * </p>
 *
 * @param aprobadas      cantidad de decisiones APPROVED
 * @param rechazadas     cantidad de decisiones REJECTED
 * @param revisionManual cantidad de decisiones MANUAL_REVIEW
 * @param escaladas      cantidad de decisiones ESCALATED
 * @param pctAprobacion  porcentaje de aprobaciones (2 decimales)
 * @param pctRechazo     porcentaje de rechazos (2 decimales)
 * @param pctManual      porcentaje de revisiones manuales (2 decimales)
 * @param pctEscalado    porcentaje de escalados (2 decimales)
 */
public record DistribucionDecisiones(
        long aprobadas,
        long rechazadas,
        long revisionManual,
        long escaladas,
        BigDecimal pctAprobacion,
        BigDecimal pctRechazo,
        BigDecimal pctManual,
        BigDecimal pctEscalado
) {}
