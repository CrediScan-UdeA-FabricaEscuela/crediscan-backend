package co.udea.codefactory.creditscoring.shared.security.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuditLogFilter(
        Instant from,
        Instant to,
        String actor,
        String action,
        String entityType,
        UUID entityId,
        String actorIp) {
}
