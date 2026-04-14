package co.udea.codefactory.creditscoring.scoringmodel.domain.port.in;

import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;

public interface ActivateScoringModelUseCase {

    /**
     * Activa la versión del modelo identificada por {@code id}.
     * Automáticamente desactiva el modelo previamente activo (CA4).
     * Genera el snapshot de variables y rangos en el momento de la activación.
     *
     * @param id UUID del modelo a activar
     * @return el modelo ya activado
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException si no existe
     * @throws co.udea.codefactory.creditscoring.scoringmodel.domain.exception.ScoringModelValidationException
     *         si no cumple RN1 (pesos ≠ 1.00), RN2 (menos de 3 variables) o no está en DRAFT
     */
    ScoringModel activar(UUID id);
}
