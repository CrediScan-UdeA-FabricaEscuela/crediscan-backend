package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;

/**
 * Puerto de salida para la persistencia de evaluaciones crediticias.
 */
public interface EvaluationRepositoryPort {

    /** Persiste o actualiza una evaluación y retorna la entidad guardada. */
    Evaluation save(Evaluation evaluation);

    /** Busca una evaluación por su identificador único. */
    Optional<Evaluation> findById(UUID id);

    /**
     * Verifica si existe una evaluación para el solicitante evaluada después de {@code since}.
     * Se usa para verificar el cooldown antes de iniciar una nueva evaluación.
     */
    boolean existsByApplicantIdAndEvaluatedAtAfter(UUID applicantId, OffsetDateTime since);
}
