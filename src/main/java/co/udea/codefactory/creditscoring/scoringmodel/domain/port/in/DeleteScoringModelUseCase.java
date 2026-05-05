package co.udea.codefactory.creditscoring.scoringmodel.domain.port.in;

import java.util.UUID;

public interface DeleteScoringModelUseCase {

    /** Elimina el modelo de scoring con el id indicado. Solo modelos en estado DRAFT pueden eliminarse. */
    void eliminar(UUID id);
}
