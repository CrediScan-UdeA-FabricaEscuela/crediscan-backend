package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta para las estadísticas de búsqueda de evaluaciones.
 *
 * <p>Reutiliza {@link LevelCountDto} (ya existe desde HU-011) para la distribución por nivel.</p>
 */
public record EvaluationStatsResponse(
        long total,
        BigDecimal averageScore,
        List<LevelCountDto> distribution
) {}
