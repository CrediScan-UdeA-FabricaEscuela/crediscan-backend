package co.udea.codefactory.creditscoring.evaluation.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ítem de clasificación: la evaluación más reciente de un solicitante
 * dentro de un rango de fechas y nivel de riesgo.
 */
public record ClassificationItem(
        UUID evaluationId,
        UUID applicantId,
        String applicantName,
        BigDecimal score,
        RiskLevel level,
        OffsetDateTime evaluatedAt,
        String evaluatedBy) {
}
