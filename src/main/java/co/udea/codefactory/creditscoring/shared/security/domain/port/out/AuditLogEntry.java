package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

import java.util.UUID;

public record AuditLogEntry(
        String entityType,
        UUID entityId,
        String action,
        String actor,
        String actorIp,
        String result,
        Object dataBefore,
        Object dataAfter) {

    public static AuditLogEntry of(String entityType, UUID entityId, String action, String actor,
                                   Object dataBefore, Object dataAfter) {
        return new AuditLogEntry(entityType, entityId, action, actor, null, "SUCCESS", dataBefore, dataAfter);
    }

    public static AuditLogEntry of(String entityType, UUID entityId, String action, String actor,
                                   String actorIp, String result, Object dataBefore, Object dataAfter) {
        return new AuditLogEntry(entityType, entityId, action, actor, actorIp, result, dataBefore, dataAfter);
    }
}
