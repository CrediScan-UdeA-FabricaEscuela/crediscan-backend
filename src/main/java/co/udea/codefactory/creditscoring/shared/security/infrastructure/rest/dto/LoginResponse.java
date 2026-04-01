package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto;

import java.time.Instant;

import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;

public record LoginResponse(
        String token,
        Role role,
        Instant expiresAt) {
}
