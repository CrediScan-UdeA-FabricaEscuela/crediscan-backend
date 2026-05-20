package co.udea.codefactory.creditscoring.evaluation.domain.model;

import java.math.BigDecimal;

/**
 * Comparación entre dos evaluaciones del mismo solicitante.
 * {@code scoreDelta} = {@code eval2.totalScore - eval1.totalScore}.
 */
public record EvaluationComparison(
        EvaluationDetailView eval1,
        EvaluationDetailView eval2,
        BigDecimal scoreDelta) {
}
