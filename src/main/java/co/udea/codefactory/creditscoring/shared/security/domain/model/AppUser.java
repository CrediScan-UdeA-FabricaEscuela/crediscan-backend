package co.udea.codefactory.creditscoring.shared.security.domain.model;

import java.util.UUID;

public record AppUser(
        UUID id,
        String username,
        String email,
        String passwordHash,
        Role role,
        boolean enabled,
        boolean accountLocked) {
}
