package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla evaluation_knockout.
 * Persiste el resultado de cada regla knockout evaluada durante la evaluación.
 */
@Entity
@Table(name = "evaluation_knockout")
public class EvaluationKnockoutJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private EvaluationJpaEntity evaluation;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "field_value", nullable = false, length = 255)
    private String fieldValue;

    @Column(name = "triggered", nullable = false)
    private boolean triggered;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // Constructor sin args requerido por JPA
    protected EvaluationKnockoutJpaEntity() {}

    public EvaluationKnockoutJpaEntity(UUID id, UUID ruleId, String ruleName,
            String fieldValue, boolean triggered, OffsetDateTime createdAt) {
        this.id = id;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.fieldValue = fieldValue;
        this.triggered = triggered;
        this.createdAt = createdAt;
    }

    // Setter para la relación bidireccional
    public void setEvaluation(EvaluationJpaEntity evaluation) {
        this.evaluation = evaluation;
    }

    // Getters
    public UUID getId() { return id; }
    public EvaluationJpaEntity getEvaluation() { return evaluation; }
    public UUID getRuleId() { return ruleId; }
    public String getRuleName() { return ruleName; }
    public String getFieldValue() { return fieldValue; }
    public boolean isTriggered() { return triggered; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
