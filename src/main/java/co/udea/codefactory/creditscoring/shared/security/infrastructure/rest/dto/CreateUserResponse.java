package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto;

import java.util.UUID;

import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;

public record CreateUserResponse(
        UUID id,
        String username,
        String email,
        Role role) {
}
