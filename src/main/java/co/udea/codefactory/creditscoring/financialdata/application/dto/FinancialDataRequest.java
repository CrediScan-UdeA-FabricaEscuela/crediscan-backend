package co.udea.codefactory.creditscoring.financialdata.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FinancialDataRequest(
        @NotNull(message = "El ingreso anual es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El ingreso anual no puede ser negativo")
        BigDecimal annualIncome,

        @NotNull(message = "Los gastos mensuales son obligatorios")
        @DecimalMin(value = "0.00", inclusive = true, message = "Los gastos mensuales no pueden ser negativos")
        BigDecimal monthlyExpenses,

        @NotNull(message = "La deuda total actual es obligatoria")
        @DecimalMin(value = "0.00", inclusive = true, message = "La deuda total no puede ser negativa")
        BigDecimal currentDebts,

        @NotNull(message = "El valor de activos es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El valor de activos no puede ser negativo")
        BigDecimal assetsValue,

        @NotNull(message = "El patrimonio declarado es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El patrimonio declarado no puede ser negativo")
        BigDecimal declaredPatrimony,

        boolean hasOutstandingDefaults,

        @NotNull(message = "El historial de crédito en meses es obligatorio")
        @Min(value = 0, message = "El historial de crédito no puede ser negativo")
        Integer creditHistoryMonths,

        @NotNull(message = "Las moras en los últimos 12 meses son obligatorias")
        @Min(value = 0, message = "Las moras en los últimos 12 meses no pueden ser negativas")
        Integer defaultsLast12m,

        @NotNull(message = "Las moras en los últimos 24 meses son obligatorias")
        @Min(value = 0, message = "Las moras en los últimos 24 meses no pueden ser negativas")
        Integer defaultsLast24m,

        @Min(value = 0, message = "El score de buró no puede ser negativo")
        @Max(value = 999, message = "El score de buró debe ser menor o igual a 999")
        Integer externalBureauScore,

        @NotNull(message = "El número de productos crediticios vigentes es obligatorio")
        @Min(value = 0, message = "El número de productos crediticios vigentes no puede ser negativo")
        Integer activeCreditProducts) {
}
