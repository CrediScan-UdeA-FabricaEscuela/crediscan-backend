package co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para registrar una decisión crediticia.
 */
public record RegisterCreditDecisionRequest(
        @NotBlank(message = "El estado de decisión es obligatorio")
        String decision,

        @NotBlank(message = "Las observaciones son obligatorias")
        @Size(min = 20, message = "Las observaciones deben tener al menos 20 caracteres")
        String observations
) {}
