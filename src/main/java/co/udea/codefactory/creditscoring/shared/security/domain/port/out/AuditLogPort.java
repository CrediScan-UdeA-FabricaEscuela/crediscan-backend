package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

import java.util.UUID;

public interface AuditLogPort {

    default void record(String entityType, UUID entityId, String action, String actor,
                        Object dataBefore, Object dataAfter) {
        record(entityType, entityId, action, actor, null, "SUCCESS", dataBefore, dataAfter);
    }

    void record(String entityType, UUID entityId, String action, String actor,
                String actorIp, String result, Object dataBefore, Object dataAfter);
}
