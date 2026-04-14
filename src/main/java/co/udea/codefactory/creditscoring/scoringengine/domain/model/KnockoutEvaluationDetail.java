package co.udea.codefactory.creditscoring.scoringengine.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutOperator;

/**
 * Resultado de la evaluación de una regla knockout para un solicitante (CA6).
 */
public record KnockoutEvaluationDetail(
        UUID reglaId,
        String campo,
        KnockoutOperator operador,
        BigDecimal umbral,
        BigDecimal valorObservado,
        boolean activada,
        String mensaje
) {}
