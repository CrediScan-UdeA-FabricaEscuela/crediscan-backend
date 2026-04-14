package co.udea.codefactory.creditscoring.scoringmodel.domain.port.in;

import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.ScoringModelComparisonResponse;

public interface CompareScoringModelsUseCase {

    /**
     * Compara dos versiones del modelo de scoring (CA6).
     * Muestra variables agregadas, eliminadas y modificadas entre las dos versiones.
     *
     * @param idBase UUID del modelo que actúa como base de la comparación
     * @param idComparado UUID del modelo que se compara contra la base
     * @return diferencias entre ambas versiones
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException
     *         si cualquiera de los dos modelos no existe
     */
    ScoringModelComparisonResponse comparar(UUID idBase, UUID idComparado);
}
