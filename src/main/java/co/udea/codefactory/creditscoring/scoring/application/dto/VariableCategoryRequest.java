package co.udea.codefactory.creditscoring.scoring.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Categoría de scoring para variables categóricas. */
public record VariableCategoryRequest(

        @NotBlank(message = "El valor de la categoría es obligatorio")
        String categoria,

        @Min(value = 0, message = "El puntaje de la categoría debe ser mayor o igual a 0")
        @Max(value = 100, message = "El puntaje de la categoría debe ser menor o igual a 100")
        int puntaje,

        String etiqueta) {
}
