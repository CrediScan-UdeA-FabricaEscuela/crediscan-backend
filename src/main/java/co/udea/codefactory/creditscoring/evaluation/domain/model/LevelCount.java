package co.udea.codefactory.creditscoring.evaluation.domain.model;

/**
 * Conteo de evaluaciones para un nivel de riesgo específico.
 */
public record LevelCount(RiskLevel level, long count) {
}
