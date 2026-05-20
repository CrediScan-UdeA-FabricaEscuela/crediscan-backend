package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

/**
 * DTO de respuesta para la comparación de dos evaluaciones del mismo solicitante.
 * {@code scoreDelta} = eval2.totalScore - eval1.totalScore.
 */
public record EvaluationComparisonResponse(
        EvaluationDetailResponse eval1,
        EvaluationDetailResponse eval2,
        BigDecimal scoreDelta) {
}
