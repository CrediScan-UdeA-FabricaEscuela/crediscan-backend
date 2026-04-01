package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AuditLogId implements Serializable {

    private UUID id;
    private Instant createdAt;

    public AuditLogId() {}

    public AuditLogId(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLogId that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, createdAt);
    }
}
