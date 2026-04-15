package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA para la entidad EvaluationJpaEntity.
 */
public interface JpaEvaluationRepository extends JpaRepository<EvaluationJpaEntity, UUID> {

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
}
