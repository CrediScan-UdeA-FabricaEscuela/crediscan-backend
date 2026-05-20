package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.udea.codefactory.creditscoring.evaluation.domain.port.out.LatestEvaluationProjection;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.RiskLevelCountProjection;

/**
 * Repositorio Spring Data JPA para la entidad EvaluationJpaEntity.
 */
public interface JpaEvaluationRepository extends JpaRepository<EvaluationJpaEntity, UUID> {

    @Query("SELECT e FROM EvaluationJpaEntity e ORDER BY e.evaluatedAt DESC")
    List<EvaluationJpaEntity> findAllByOrderByEvaluatedAtDesc();

    /**
     * Verifica si existe una evaluación para el solicitante evaluada después de {@code since}.
     * Usado para la validación del período de cooldown.
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
           "FROM EvaluationJpaEntity e " +
           "WHERE e.applicantId = :applicantId AND e.evaluatedAt > :since")
    boolean existsByApplicantIdAndEvaluatedAtAfter(
            @Param("applicantId") UUID applicantId,
            @Param("since") OffsetDateTime since);

    /**
     * Cuenta la última evaluación por solicitante agrupada por nivel de riesgo.
     * Usa ROW_NUMBER() para seleccionar solo la evaluación más reciente por applicant.
     */
    @Query(value = """
            SELECT r.risk_level AS level, COUNT(*) AS total
            FROM (
              SELECT e.applicant_id, e.risk_level,
                     ROW_NUMBER() OVER (PARTITION BY e.applicant_id
                                        ORDER BY e.evaluated_at DESC, e.id DESC) AS rn
              FROM evaluation e
              WHERE e.evaluated_at BETWEEN :desde AND :hasta
            ) r
            WHERE r.rn = 1
            GROUP BY r.risk_level
            """, nativeQuery = true)
    List<RiskLevelCountProjection> countLatestByRiskLevel(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta);

    /**
     * Retorna una página con la última evaluación por solicitante dentro del rango,
     * filtrada opcionalmente por nivel de riesgo. Incluye nombre del solicitante via JOIN.
     */
    @Query(value = """
            SELECT r.id            AS evaluationId,
                   r.applicant_id  AS applicantId,
                   a.name          AS applicantName,
                   r.total_score   AS totalScore,
                   r.risk_level    AS riskLevel,
                   r.evaluated_at  AS evaluatedAt,
                   r.evaluated_by  AS evaluatedBy
            FROM (
              SELECT e.*,
                     ROW_NUMBER() OVER (PARTITION BY e.applicant_id
                                        ORDER BY e.evaluated_at DESC, e.id DESC) AS rn
              FROM evaluation e
              WHERE e.evaluated_at BETWEEN :desde AND :hasta
            ) r
            JOIN applicant a ON a.id = r.applicant_id
            WHERE r.rn = 1
              AND (:nivel IS NULL OR r.risk_level = :nivel)
            ORDER BY r.evaluated_at DESC, r.id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
              SELECT e.applicant_id,
                     ROW_NUMBER() OVER (PARTITION BY e.applicant_id
                                        ORDER BY e.evaluated_at DESC, e.id DESC) AS rn,
                     e.risk_level
              FROM evaluation e
              WHERE e.evaluated_at BETWEEN :desde AND :hasta
            ) r
            WHERE r.rn = 1
              AND (:nivel IS NULL OR r.risk_level = :nivel)
            """,
            nativeQuery = true)
    Page<LatestEvaluationProjection> findLatestPerApplicantInRange(
            @Param("nivel") String nivel,
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            Pageable pageable);

    /**
     * Retorna todas las evaluaciones de un solicitante ordenadas por fecha y id descendentes.
     */
    @Query("SELECT e FROM EvaluationJpaEntity e " +
           "WHERE e.applicantId = :applicantId " +
           "ORDER BY e.evaluatedAt DESC, e.id DESC")
    List<EvaluationJpaEntity> findByApplicantIdOrderByEvaluatedAtDescIdDesc(
            @Param("applicantId") UUID applicantId);
}
