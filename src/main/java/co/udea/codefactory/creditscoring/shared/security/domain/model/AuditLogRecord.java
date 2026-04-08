package co.udea.codefactory.creditscoring.shared.security.domain.model;

import java.time.Instant;
import java.util.UUID;

public class AuditLogRecord {

    private final UUID id;
    private final Instant createdAt;
    private final String entityType;
    private final UUID entityId;
    private final String action;
    private final String actor;
    private final String actorIp;
    private final String result;
    private final String dataBefore;
    private final String dataAfter;
    private final String details;

    public AuditLogRecord(UUID id,
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
        this.id = id;
        this.createdAt = createdAt;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actor = actor;
        this.actorIp = actorIp;
        this.result = result;
        this.dataBefore = dataBefore;
        this.dataAfter = dataAfter;
        this.details = details;
    }

    public UUID id() {
        return id;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String entityType() {
        return entityType;
    }

    public UUID entityId() {
        return entityId;
    }

    public String action() {
        return action;
    }

    public String actor() {
        return actor;
    }

    public String actorIp() {
        return actorIp;
    }

    public String result() {
        return result;
    }

    public String dataBefore() {
        return dataBefore;
    }

    public String dataAfter() {
        return dataAfter;
    }

    public String details() {
        return details;
    }
}
