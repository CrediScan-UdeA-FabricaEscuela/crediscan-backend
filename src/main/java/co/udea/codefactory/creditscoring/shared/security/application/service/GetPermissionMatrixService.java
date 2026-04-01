package co.udea.codefactory.creditscoring.shared.security.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.shared.security.domain.model.RolePermission;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.GetPermissionMatrixUseCase;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.RolePermissionPort;

@Service
public class GetPermissionMatrixService implements GetPermissionMatrixUseCase {

    private final RolePermissionPort rolePermissionPort;

    public GetPermissionMatrixService(RolePermissionPort rolePermissionPort) {
        this.rolePermissionPort = rolePermissionPort;
    }

    @Override
    public List<RolePermission> getAll() {
        return rolePermissionPort.findAll();
    }
}
