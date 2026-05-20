package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Proyección Spring Data para la búsqueda avanzada de evaluaciones.
 * Usada por la native query {@code searchEvaluations} de {@code JpaEvaluationRepository}.
 *
 * <p>Nota: {@code evaluatedAt} se declara como {@code Instant} porque Hibernate proyecta
 * columnas TIMESTAMP WITH TIME ZONE a Instant en native queries. La conversión a
 * {@code OffsetDateTime} se realiza en el adapter.</p>
 */
public interface EvaluationSearchItemProjection {

    UUID getEvaluationId();
    UUID getApplicantId();
    String getApplicantName();
    Instant getEvaluatedAt();
    BigDecimal getScore();
    String getRiskLevel();
    String getDecisionStatus();
    String getAnalista();
}
