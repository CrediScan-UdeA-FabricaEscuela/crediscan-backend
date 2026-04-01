package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuditLogRepository extends JpaRepository<JpaAuditLogEntity, AuditLogId> {
}
