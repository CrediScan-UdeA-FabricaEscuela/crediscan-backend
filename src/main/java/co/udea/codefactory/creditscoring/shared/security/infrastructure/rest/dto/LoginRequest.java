package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
