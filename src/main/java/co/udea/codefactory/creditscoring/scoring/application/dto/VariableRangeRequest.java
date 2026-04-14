package co.udea.codefactory.creditscoring.scoring.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Rango de valoración para variables numéricas. */
public record VariableRangeRequest(

        @NotNull(message = "El límite inferior del rango es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El límite inferior no puede ser negativo")
        BigDecimal limiteInferior,

        @NotNull(message = "El límite superior del rango es obligatorio")
        BigDecimal limiteSuperior,

        @Min(value = 0, message = "El puntaje del rango debe ser mayor o igual a 0")
        @Max(value = 100, message = "El puntaje del rango debe ser menor o igual a 100")
        int puntaje,

        String etiqueta) {
}
