package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence.efectividad;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.EvaluationJpaEntity;

/**
 * Repositorio Spring Data JPA para la query de la matriz de confusión (HU-016).
 * <p>
 * Extiende {@code JpaRepository<EvaluationJpaEntity, UUID>} para acceder a native queries
 * sin crear una nueva entidad JPA en el BC reporting (patrón HU-015).
 * </p>
 */
public interface EfectividadModeloJpaRepository extends JpaRepository<EvaluationJpaEntity, UUID> {

    /**
     * Retorna los conteos de evaluaciones agrupados por risk_level × decision.
     * Solo incluye evaluaciones con credit_decision (INNER JOIN).
     * El parámetro analistaId es opcional (null = todos).
     */
    @Query(value = """
            SELECT e.risk_level AS riskLevel, cd.decision AS decision, COUNT(*) AS count
            FROM evaluation e
            INNER JOIN credit_decision cd ON cd.evaluation_id = e.id
            WHERE e.evaluated_at BETWEEN :desde AND :hasta
              AND (:analistaId IS NULL OR e.evaluated_by = CAST(:analistaId AS text))
            GROUP BY e.risk_level, cd.decision
            """, nativeQuery = true)
    List<MatrizRawProjection> queryMatriz(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            @Param("analistaId") String analistaId);
}
