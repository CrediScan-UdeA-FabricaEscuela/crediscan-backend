package co.udea.codefactory.creditscoring.scoring.domain.port.in;

import co.udea.codefactory.creditscoring.scoring.application.dto.CreateScoringVariableRequest;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;

public interface CreateScoringVariableUseCase {

    /**
     * Crea y persiste una nueva variable de scoring.
     *
     * @param request datos de la variable con rangos o categorías
     * @return la variable creada con su UUID generado
     * @throws co.udea.codefactory.creditscoring.scoring.domain.exception.ScoringVariableValidationException
     *         si el nombre ya existe, el tipo/rangos son inválidos o el peso está fuera de rango
     */
    ScoringVariable crear(CreateScoringVariableRequest request);
}
