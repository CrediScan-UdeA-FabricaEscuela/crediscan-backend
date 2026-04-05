package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

import java.util.List;

import co.udea.codefactory.creditscoring.shared.security.domain.model.RolePermission;

public interface RolePermissionPort {

    List<RolePermission> findAll();
}
