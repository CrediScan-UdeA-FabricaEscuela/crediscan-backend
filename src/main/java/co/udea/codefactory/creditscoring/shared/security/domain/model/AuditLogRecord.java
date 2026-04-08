package co.udea.codefactory.creditscoring.shared.security.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuditLogRecord(
        UUID id,
        Instant createdAt,
        String entityType,
        UUID entityId,
        String action,
        String actor,
        String actorIp,
        String result,
        String dataBefore,
        String dataAfter,
        String details) {
}
