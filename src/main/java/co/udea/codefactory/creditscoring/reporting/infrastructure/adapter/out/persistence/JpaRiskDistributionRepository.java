package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.EvaluationJpaEntity;

/**
 * Repositorio Spring Data JPA para queries analíticas de distribución de riesgo.
 * Extiende JpaRepository de EvaluationJpaEntity para acceder a native queries
 * sin crear una nueva entidad JPA en el BC reporting.
 *
 * <p>Las tres queries usan CTE "ranked" con ROW_NUMBER PARTITION BY applicant_id
 * para contar solo la evaluación más reciente por solicitante.</p>
 */
public interface JpaRiskDistributionRepository extends JpaRepository<EvaluationJpaEntity, UUID> {

    @Query(value = """
            WITH ranked AS (
              SELECT e.id, e.applicant_id, e.total_score, e.risk_level,
                     ROW_NUMBER() OVER (PARTITION BY e.applicant_id ORDER BY e.evaluated_at DESC, e.id DESC) AS rn
              FROM evaluation e
              JOIN applicant a ON a.id = e.applicant_id
              WHERE e.evaluated_at BETWEEN :desde AND :hasta
                AND (:tipoEmpleo IS NULL OR UPPER(a.employment_type) = UPPER(CAST(:tipoEmpleo AS text)))
            )
            SELECT r.risk_level AS level, COUNT(*) AS cnt, AVG(r.total_score) AS avgScore
            FROM ranked r WHERE r.rn = 1
            GROUP BY r.risk_level
            """, nativeQuery = true)
    List<LevelCountProjection> distributionByLevel(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            @Param("tipoEmpleo") String tipoEmpleo);

    @Query(value = """
            WITH ranked AS (
              SELECT e.id, e.applicant_id, e.total_score, e.risk_level,
                     ROW_NUMBER() OVER (PARTITION BY e.applicant_id ORDER BY e.evaluated_at DESC, e.id DESC) AS rn
              FROM evaluation e
              JOIN applicant a ON a.id = e.applicant_id
              WHERE e.evaluated_at BETWEEN :desde AND :hasta
                AND (:tipoEmpleo IS NULL OR UPPER(a.employment_type) = UPPER(CAST(:tipoEmpleo AS text)))
            )
            SELECT LEAST(FLOOR(r.total_score / 10) * 10, 90)::int AS binStart, COUNT(*) AS cnt
            FROM ranked r WHERE r.rn = 1
            GROUP BY binStart
            ORDER BY binStart
            """, nativeQuery = true)
    List<BinCountProjection> histogram(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            @Param("tipoEmpleo") String tipoEmpleo);

    @Query(value = """
            WITH ranked AS (
              SELECT e.id, e.applicant_id, e.total_score, e.risk_level,
                     ROW_NUMBER() OVER (PARTITION BY e.applicant_id ORDER BY e.evaluated_at DESC, e.id DESC) AS rn
              FROM evaluation e
              JOIN applicant a ON a.id = e.applicant_id
              WHERE e.evaluated_at BETWEEN :desde AND :hasta
                AND (:tipoEmpleo IS NULL OR UPPER(a.employment_type) = UPPER(CAST(:tipoEmpleo AS text)))
            )
            SELECT COUNT(*) AS total,
                   COALESCE(AVG(r.total_score), 0) AS avg,
                   COALESCE(STDDEV_POP(r.total_score), 0) AS stddev
            FROM ranked r WHERE r.rn = 1
            """, nativeQuery = true)
    OverallStatsProjection overallStats(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            @Param("tipoEmpleo") String tipoEmpleo);
}
