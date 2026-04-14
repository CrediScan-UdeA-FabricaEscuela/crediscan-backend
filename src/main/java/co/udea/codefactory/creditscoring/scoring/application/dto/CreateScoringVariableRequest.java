package co.udea.codefactory.creditscoring.scoring.application.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos de entrada para crear una variable de scoring. */
public record CreateScoringVariableRequest(

        @NotBlank(message = "El nombre de la variable es obligatorio")
        String nombre,

        String descripcion,

        @NotBlank(message = "El tipo de variable es obligatorio (NUMERIC o CATEGORICAL)")
        String tipo,

        @NotNull(message = "El peso de la variable es obligatorio")
        @DecimalMin(value = "0.01", message = "El peso mínimo permitido es 0.01")
        @DecimalMax(value = "1.00", message = "El peso máximo permitido es 1.00")
        BigDecimal peso,

        @Valid
        List<VariableRangeRequest> rangos,

        @Valid
        List<VariableCategoryRequest> categorias) {
}
