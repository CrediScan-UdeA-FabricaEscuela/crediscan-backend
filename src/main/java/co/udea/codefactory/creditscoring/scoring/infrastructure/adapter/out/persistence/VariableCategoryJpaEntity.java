package co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "variable_category")
public class VariableCategoryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "variable_id", nullable = false)
    private UUID variableId;

    @Column(name = "category_value", nullable = false)
    private String categoryValue;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "label")
    private String label;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVariableId() {
        return variableId;
    }

    public void setVariableId(UUID variableId) {
        this.variableId = variableId;
    }

    public String getCategoryValue() {
        return categoryValue;
    }

    public void setCategoryValue(String categoryValue) {
        this.categoryValue = categoryValue;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
