package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

import co.udea.codefactory.creditscoring.shared.security.domain.model.Role;

public record ChangeRoleRequest(
        @NotNull Role rol) {
}
