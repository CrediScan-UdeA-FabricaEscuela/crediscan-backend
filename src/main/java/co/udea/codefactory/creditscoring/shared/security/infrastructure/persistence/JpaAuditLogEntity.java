package co.udea.codefactory.creditscoring.shared.security.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_log")
public class JpaAuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(name = "actor_ip", length = 45)
    private String actorIp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_before", columnDefinition = "jsonb")
    private String dataBefore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_after", columnDefinition = "jsonb")
    private String dataAfter;

    @Column(length = 1000)
    private String details;

    protected JpaAuditLogEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getActorIp() { return actorIp; }
    public void setActorIp(String actorIp) { this.actorIp = actorIp; }

    public String getDataBefore() { return dataBefore; }
    public void setDataBefore(String dataBefore) { this.dataBefore = dataBefore; }

    public String getDataAfter() { return dataAfter; }
    public void setDataAfter(String dataAfter) { this.dataAfter = dataAfter; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
