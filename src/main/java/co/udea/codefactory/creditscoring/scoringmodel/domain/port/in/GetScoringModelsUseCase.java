package co.udea.codefactory.creditscoring.scoringmodel.domain.port.in;

import java.util.List;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;

public interface GetScoringModelsUseCase {

    /** Retorna todos los modelos de scoring ordenados por versión descendente. */
    List<ScoringModel> listar();

    /**
     * Retorna el modelo con el UUID indicado.
     *
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException si no existe
     */
    ScoringModel obtener(UUID id);
}
