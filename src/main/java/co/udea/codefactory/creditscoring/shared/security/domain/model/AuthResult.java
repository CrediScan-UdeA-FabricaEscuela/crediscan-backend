package co.udea.codefactory.creditscoring.shared.security.domain.model;

import java.time.Instant;

public record AuthResult(
        String token,
        Role role,
        Instant expiresAt) {
}
