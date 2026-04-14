package co.udea.codefactory.creditscoring.scoringmodel.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;

public interface ScoringModelRepositoryPort {

    /** Persiste un nuevo modelo con sus variables. */
    ScoringModel save(ScoringModel modelo);

    /** Actualiza un modelo existente reemplazando sus variables. */
    ScoringModel update(ScoringModel modelo);

    Optional<ScoringModel> findById(UUID id);

    /** Retorna todos los modelos ordenados por versión descendente. */
    List<ScoringModel> findAll();

    /** Retorna el modelo en estado ACTIVE, si existe. */
    Optional<ScoringModel> findActive();

    /** Retorna el número de versión máximo existente (0 si no hay ninguno). */
    int maxVersion();

    boolean existsByNombre(String nombre);
}
