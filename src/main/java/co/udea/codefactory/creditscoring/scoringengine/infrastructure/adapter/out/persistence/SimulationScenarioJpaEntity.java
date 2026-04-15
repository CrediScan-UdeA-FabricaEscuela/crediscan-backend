package co.udea.codefactory.creditscoring.scoringengine.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "simulation_scenario")
public class SimulationScenarioJpaEntity {

    @Id
    private UUID id;

    @Column(name = "model_id", nullable = false)
    private UUID modelId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    /** Valores de las variables en formato JSONB (mapa nombre → valor). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "values_snapshot", columnDefinition = "jsonb", nullable = false)
    private String valuesSnapshot;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getValuesSnapshot() { return valuesSnapshot; }
    public void setValuesSnapshot(String valuesSnapshot) { this.valuesSnapshot = valuesSnapshot; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
