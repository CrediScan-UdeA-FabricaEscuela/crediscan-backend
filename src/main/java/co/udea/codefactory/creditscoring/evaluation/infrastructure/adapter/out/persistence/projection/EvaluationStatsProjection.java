package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.projection;

import java.math.BigDecimal;

/**
 * Proyección Spring Data para las estadísticas de búsqueda de evaluaciones.
 * Usada por la native query {@code statsByCriteria} de {@code JpaEvaluationRepository}.
 */
public interface EvaluationStatsProjection {

    long getTotal();
    BigDecimal getAvgScore();
    long getCountVeryLow();
    long getCountLow();
    long getCountMedium();
    long getCountHigh();
    long getCountVeryHigh();
    long getCountRejected();
}
