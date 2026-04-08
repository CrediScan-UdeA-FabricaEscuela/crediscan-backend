package co.udea.codefactory.creditscoring.shared.security.domain.model;

import java.time.Instant;
import java.util.UUID;

public class AuditLogFilter {

    private final Instant from;
    private final Instant to;
    private final String actor;
    private final String action;
    private final String entityType;
    private final UUID entityId;
    private final String actorIp;

    public AuditLogFilter(Instant from, Instant to, String actor, String action,
                          String entityType, UUID entityId, String actorIp) {
        this.from = from;
        this.to = to;
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorIp = actorIp;
    }

    public Instant from() {
        return from;
    }

    public Instant to() {
        return to;
    }

    public String actor() {
        return actor;
    }

    public String action() {
        return action;
    }

    public String entityType() {
        return entityType;
    }

    public UUID entityId() {
        return entityId;
    }

    public String actorIp() {
        return actorIp;
    }
}
