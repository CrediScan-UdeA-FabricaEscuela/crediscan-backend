package co.udea.codefactory.creditscoring.scoring.domain.model;

import java.util.UUID;

import co.udea.codefactory.creditscoring.scoring.domain.exception.ScoringVariableValidationException;

/**
 * Categoría de una variable categórica de scoring con su puntaje directo.
 * Ejemplo: para la variable "Tipo de empleo", la categoría "Empleado" tiene puntaje 80.
 */
public record VariableCategory(
        UUID id,
        UUID variableId,
        String categoria,
        int puntaje,
        String etiqueta) {

    public VariableCategory {
        if (categoria == null || categoria.isBlank()) {
            throw new ScoringVariableValidationException(
                    "El valor de la categoría no puede estar vacío");
        }
        if (puntaje < 0 || puntaje > 100) {
            throw new ScoringVariableValidationException(
                    "El puntaje de la categoría debe estar entre 0 y 100 (valor recibido: " + puntaje + ")");
        }
    }
}
