package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Proyección de la última evaluación por solicitante.
 * Usada en la query paginada de clasificación por nivel de riesgo.
 */
public interface LatestEvaluationProjection {

    UUID getEvaluationId();

    UUID getApplicantId();

    String getApplicantName();

    BigDecimal getTotalScore();

    String getRiskLevel();

    OffsetDateTime getEvaluatedAt();

    String getEvaluatedBy();
}
