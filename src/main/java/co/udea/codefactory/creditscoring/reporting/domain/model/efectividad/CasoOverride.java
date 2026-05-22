package co.udea.codefactory.creditscoring.reporting.domain.model.efectividad;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.DecisionStatus;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Caso de override: evaluación en la que el analista tomó una decisión contraria
 * a la recomendación del modelo automático.
 *
 * @param riskLevel nivel de riesgo del modelo
 * @param decision  decisión del analista
 * @param count     cantidad de evaluaciones en esta combinación
 */
public record CasoOverride(
        RiskLevel riskLevel,
        DecisionStatus decision,
        long count
) {}
