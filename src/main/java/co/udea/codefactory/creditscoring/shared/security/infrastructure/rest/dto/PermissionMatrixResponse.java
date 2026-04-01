package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto;

import java.util.List;

import co.udea.codefactory.creditscoring.shared.security.domain.model.RolePermission;

public record PermissionMatrixResponse(List<RolePermission> permissions) {
}
