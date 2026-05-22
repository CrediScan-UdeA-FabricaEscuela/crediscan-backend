package co.udea.codefactory.creditscoring.reporting.domain.model.efectividad;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.DecisionStatus;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Celda de la matriz de confusión.
 * <p>
 * Representa la intersección entre un nivel de riesgo del modelo automático
 * y una decisión del analista humano. El campo {@code riskLevel} nunca es REJECTED
 * en la matriz de concordancia — REJECTED se agrupa bajo VERY_HIGH.
 * </p>
 *
 * @param riskLevel nivel de riesgo (fila de la matriz)
 * @param decision  decisión del analista (columna de la matriz)
 * @param count     cantidad de evaluaciones en esa celda
 */
public record CeldaMatriz(
        RiskLevel riskLevel,
        DecisionStatus decision,
        long count
) {}
