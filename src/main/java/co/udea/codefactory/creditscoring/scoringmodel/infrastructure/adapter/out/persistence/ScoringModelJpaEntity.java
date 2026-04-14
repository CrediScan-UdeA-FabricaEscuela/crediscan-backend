package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scoring_model")
public class ScoringModelJpaEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "deprecated_at")
    private OffsetDateTime deprecatedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // Campos requeridos por NOT NULL en DB pero no usados en HU-007
    @Column(name = "min_score", nullable = false)
    private java.math.BigDecimal minScore = java.math.BigDecimal.ZERO;

    @Column(name = "max_score", nullable = false)
    private java.math.BigDecimal maxScore = new java.math.BigDecimal("100");

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(OffsetDateTime activatedAt) { this.activatedAt = activatedAt; }
    public OffsetDateTime getDeprecatedAt() { return deprecatedAt; }
    public void setDeprecatedAt(OffsetDateTime deprecatedAt) { this.deprecatedAt = deprecatedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public java.math.BigDecimal getMinScore() { return minScore; }
    public void setMinScore(java.math.BigDecimal minScore) { this.minScore = minScore; }
    public java.math.BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(java.math.BigDecimal maxScore) { this.maxScore = maxScore; }
}
