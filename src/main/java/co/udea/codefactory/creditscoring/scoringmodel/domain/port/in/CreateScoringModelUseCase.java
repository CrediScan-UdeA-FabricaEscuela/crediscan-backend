package co.udea.codefactory.creditscoring.scoringmodel.domain.port.in;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.CreateScoringModelRequest;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;

public interface CreateScoringModelUseCase {

    /**
     * Crea una nueva versión del modelo de scoring en estado BORRADOR.
     * Si {@code request.clonarDesde()} no es nulo, clona las variables del modelo origen (CA1).
     * En caso contrario, incluye todas las variables de scoring activas con sus pesos actuales.
     *
     * @param request datos de la nueva versión
     * @return el modelo creado con su UUID y número de versión asignados
     * @throws co.udea.codefactory.creditscoring.scoringmodel.domain.exception.ScoringModelValidationException
     *         si el nombre ya existe o el modelo origen no existe
     */
    ScoringModel crear(CreateScoringModelRequest request);
}
