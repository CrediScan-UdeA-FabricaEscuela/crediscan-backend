package co.udea.codefactory.creditscoring.evaluation.domain.model.search;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Proyección de dominio con los datos de una evaluación para el listado de búsqueda.
 *
 * <p>{@code decisionStatus} es {@code null} cuando la evaluación no tiene decisión crediticia asociada.</p>
 */
public record EvaluationSearchItem(
        UUID evaluationId,
        UUID applicantId,
        String applicantName,
        OffsetDateTime evaluatedAt,
        BigDecimal score,
        RiskLevel riskLevel,
        String decisionStatus,
        String analista
) {}
