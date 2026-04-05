package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

import java.util.UUID;

public interface AuditLogPort {

    void record(String entityType, UUID entityId, String action, String actor,
                Object dataBefore, Object dataAfter);
}
