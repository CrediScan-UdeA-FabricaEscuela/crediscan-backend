package co.udea.codefactory.creditscoring.scoring.domain.port.in;

import co.udea.codefactory.creditscoring.scoring.application.dto.ScoringVariableListResponse;

public interface GetScoringVariablesUseCase {

    /**
     * Retorna todas las variables de scoring con sus rangos/categorías,
     * la suma de pesos de variables activas y las advertencias del modelo
     * (peso != 1.00, menos de 3 variables activas).
     */
    ScoringVariableListResponse listar();
}
