package co.udea.codefactory.creditscoring.scoring.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta de listado de variables de scoring.
 * Incluye las advertencias del modelo (CA4/RN4) sin bloquear la consulta.
 */
public record ScoringVariableListResponse(
        List<ScoringVariableResponse> variables,
        BigDecimal sumaPesos,
        List<String> advertencias) {
}
