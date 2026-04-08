package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaAuditLogRepository extends JpaRepository<JpaAuditLogEntity, UUID>, JpaSpecificationExecutor<JpaAuditLogEntity> {
}
