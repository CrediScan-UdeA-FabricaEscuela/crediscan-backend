package co.udea.codefactory.creditscoring.shared.security.domain.port.in;

import java.util.List;

import co.udea.codefactory.creditscoring.shared.security.domain.model.RolePermission;

public interface GetPermissionMatrixUseCase {

    List<RolePermission> getAll();
}
