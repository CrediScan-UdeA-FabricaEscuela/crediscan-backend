package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.shared.security.domain.model.RolePermission;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.RolePermissionPort;

@Component
public class RolePermissionAdapter implements RolePermissionPort {

    private final JpaRolePermissionRepository jpaRepository;

    public RolePermissionAdapter(JpaRolePermissionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<RolePermission> findAll() {
        return jpaRepository.findAll().stream()
                .map(e -> new RolePermission(e.getRole(), e.getResource(), e.getAction()))
                .toList();
    }
}
