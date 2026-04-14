package co.udea.codefactory.creditscoring.scoring.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;

public interface ScoringVariableRepositoryPort {

    /** Persiste una nueva variable con sus rangos/categorías. */
    ScoringVariable save(ScoringVariable variable);

    /** Actualiza una variable existente reemplazando sus rangos/categorías. */
    ScoringVariable update(ScoringVariable variable);

    Optional<ScoringVariable> findById(UUID id);

    Optional<ScoringVariable> findByNombre(String nombre);

    /** Retorna todas las variables, activas e inactivas, ordenadas por nombre. */
    List<ScoringVariable> findAll();

    /** Retorna solo las variables activas. */
    List<ScoringVariable> findAllActivas();

    boolean existsByNombre(String nombre);
}
