package co.udea.codefactory.creditscoring.scoringengine.domain.port.in;

import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringengine.application.dto.CalculateScoreRequest;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;

public interface CalculateScoreUseCase {

    /**
     * Calcula el puntaje crediticio de un solicitante.
     *
     * <p>Si {@code request.modeloId()} es nulo, se usa el modelo activo.</p>
     */
    ScoringResult calcular(CalculateScoreRequest request);
}
