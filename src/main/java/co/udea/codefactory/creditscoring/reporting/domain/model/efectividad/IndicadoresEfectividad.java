package co.udea.codefactory.creditscoring.reporting.domain.model.efectividad;

import java.math.BigDecimal;

/**
 * Indicadores de efectividad del modelo de riesgo.
 * <p>
 * Todos los porcentajes tienen 2 decimales. El denominador para las tasas
 * solo incluye decisiones APPROVED y REJECTED (excluye MANUAL_REVIEW y ESCALATED).
 * </p>
 *
 * @param tasaConcordanciaGlobal  porcentaje de evaluaciones concordantes sobre decisiones activas
 * @param tasaOverrideAprobacion  porcentaje de overrides de aprobación (alto riesgo aprobado)
 * @param tasaOverrideRechazo     porcentaje de overrides de rechazo (bajo riesgo rechazado)
 * @param totalCasos              total de evaluaciones con decisión activa (APPROVED+REJECTED)
 */
public record IndicadoresEfectividad(
        BigDecimal tasaConcordanciaGlobal,
        BigDecimal tasaOverrideAprobacion,
        BigDecimal tasaOverrideRechazo,
        long totalCasos
) {}
