package co.udea.codefactory.creditscoring.creditdecision.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;

/**
 * Puerto de salida para la persistencia de decisiones crediticias.
 */
public interface CreditDecisionRepositoryPort {

    /** Persiste una decisión crediticia y retorna la entidad guardada. */
    CreditDecision save(CreditDecision creditDecision);

    /**
     * Busca una decisión por su identificador único.
     */
    Optional<CreditDecision> findById(UUID id);

    /**
     * Busca una decisión por evaluation_id.
     * Retorna la decisión si existe, vacío si no.
     */
    Optional<CreditDecision> findByEvaluationId(UUID evaluationId);

    /**
     * Verifica si ya existe una decisión para la evaluación dada.
     *
     * @param evaluationId identificador de la evaluación
     * @return true si ya existe una decisión, false en caso contrario
     */
    boolean existsByEvaluationId(UUID evaluationId);
}
