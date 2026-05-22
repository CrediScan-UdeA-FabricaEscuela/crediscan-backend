package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence.analistas;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.EvaluationJpaEntity;

/**
 * Repositorio Spring Data JPA para las queries de actividad de analistas (HU-017).
 * <p>
 * Extiende {@code JpaRepository<EvaluationJpaEntity, UUID>} para acceder a native queries
 * sin crear una nueva entidad JPA en el BC reporting (patrón HU-015).
 * </p>
 */
public interface ActividadAnalistasJpaRepository extends JpaRepository<EvaluationJpaEntity, UUID> {

    /**
     * Retorna los conteos de decisiones agrupados por analista.
     * Solo incluye evaluaciones con credit_decision (INNER JOIN).
     */
    @Query(value = """
            SELECT e.evaluated_by AS evaluatedBy,
                   COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE cd.decision = 'APPROVED') AS aprobadas,
                   COUNT(*) FILTER (WHERE cd.decision = 'REJECTED') AS rechazadas,
                   COUNT(*) FILTER (WHERE cd.decision = 'MANUAL_REVIEW') AS revisionManual,
                   COUNT(*) FILTER (WHERE cd.decision = 'ESCALATED') AS escaladas
            FROM evaluation e
            INNER JOIN credit_decision cd ON cd.evaluation_id = e.id
            WHERE e.evaluated_at BETWEEN :desde AND :hasta
            GROUP BY e.evaluated_by
            """, nativeQuery = true)
    List<AnalistaCountsProjection> queryCounts(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta);

    /**
     * Retorna los timestamps para calcular el tiempo de decisión en horas hábiles.
     * Solo incluye evaluaciones con credit_decision (INNER JOIN).
     */
    @Query(value = """
            SELECT e.evaluated_by AS evaluatedBy,
                   e.evaluated_at AS evaluatedAt,
                   cd.decided_at AS decidedAt
            FROM evaluation e
            INNER JOIN credit_decision cd ON cd.evaluation_id = e.id
            WHERE e.evaluated_at BETWEEN :desde AND :hasta
            """, nativeQuery = true)
    List<AnalistaTimestampsProjection> queryTimestamps(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta);
}
