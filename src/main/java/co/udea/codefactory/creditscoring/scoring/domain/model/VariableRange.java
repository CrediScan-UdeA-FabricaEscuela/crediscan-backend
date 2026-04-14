package co.udea.codefactory.creditscoring.scoring.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoring.domain.exception.ScoringVariableValidationException;

/**
 * Valor de valoración para un intervalo numérico dentro de una variable de scoring.
 * Representa la asignación de puntaje cuando el valor de la variable cae entre
 * {@code limiteInferior} (inclusivo) y {@code limiteSuperior} (exclusivo).
 */
public record VariableRange(
        UUID id,
        UUID variableId,
        BigDecimal limiteInferior,
        BigDecimal limiteSuperior,
        int puntaje,
        String etiqueta) {

    public VariableRange {
        if (limiteInferior == null || limiteSuperior == null) {
            throw new ScoringVariableValidationException(
                    "Los límites inferior y superior del rango son obligatorios");
        }
        if (limiteInferior.compareTo(BigDecimal.ZERO) < 0) {
            throw new ScoringVariableValidationException(
                    "El límite inferior de un rango no puede ser negativo");
        }
        if (limiteInferior.compareTo(limiteSuperior) >= 0) {
            throw new ScoringVariableValidationException(
                    "El límite inferior debe ser estrictamente menor que el límite superior");
        }
        if (puntaje < 0 || puntaje > 100) {
            throw new ScoringVariableValidationException(
                    "El puntaje del rango debe estar entre 0 y 100 (valor recibido: " + puntaje + ")");
        }
    }
}
