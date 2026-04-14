package co.udea.codefactory.creditscoring.scoring.domain.port.in;

import java.util.UUID;

import co.udea.codefactory.creditscoring.scoring.application.dto.UpdateScoringVariableRequest;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;

public interface UpdateScoringVariableUseCase {

    /**
     * Actualiza una variable de scoring existente.
     * Los cambios aplican solo para evaluaciones futuras (CA8).
     *
     * @param id      UUID de la variable a actualizar
     * @param request datos actualizados
     * @return la variable actualizada
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException si la variable no existe
     * @throws co.udea.codefactory.creditscoring.scoring.domain.exception.ScoringVariableValidationException
     *         si el nuevo nombre ya existe en otra variable o los rangos son inválidos
     */
    ScoringVariable actualizar(UUID id, UpdateScoringVariableRequest request);
}
