package co.udea.codefactory.creditscoring.evaluation.domain.model.search;

import java.math.BigDecimal;
import java.util.Map;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Estadísticas agregadas del resultado de una búsqueda de evaluaciones.
 *
 * <p>{@code averageScore} es {@code null} cuando {@code total == 0}.
 * {@code distribution} siempre contiene las 6 keys de {@link RiskLevel} con default 0.</p>
 */
public record EvaluationStats(
        long total,
        BigDecimal averageScore,
        Map<RiskLevel, Long> distribution
) {}
