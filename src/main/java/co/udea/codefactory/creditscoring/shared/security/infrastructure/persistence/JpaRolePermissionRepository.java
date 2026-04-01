package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRolePermissionRepository extends JpaRepository<JpaRolePermissionEntity, UUID> {
}
