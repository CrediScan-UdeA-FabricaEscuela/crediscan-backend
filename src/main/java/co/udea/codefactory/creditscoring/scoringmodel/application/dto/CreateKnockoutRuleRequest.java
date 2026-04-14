package co.udea.codefactory.creditscoring.scoringmodel.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateKnockoutRuleRequest(

        @NotBlank(message = "El campo de referencia es obligatorio")
        String campo,

        @NotNull(message = "El operador de comparación es obligatorio")
        String operador,

        @NotNull(message = "El valor umbral es obligatorio")
        BigDecimal umbral,

        @NotBlank(message = "El mensaje descriptivo es obligatorio")
        String mensaje,

        @Min(value = 0, message = "La prioridad no puede ser negativa")
        int prioridad
) {}
