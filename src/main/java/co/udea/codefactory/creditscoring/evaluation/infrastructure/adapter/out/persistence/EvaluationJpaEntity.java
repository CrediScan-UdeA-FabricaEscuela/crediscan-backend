package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla evaluation.
 * Es el agregado raíz persistido y contiene las relaciones con detail y knockout.
 */
@Entity
@Table(name = "evaluation")
public class EvaluationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "applicant_id", nullable = false)
    private UUID applicantId;

    @Column(name = "model_id", nullable = false)
    private UUID modelId;

    @Column(name = "financial_data_id", nullable = false)
    private UUID financialDataId;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "knocked_out", nullable = false)
    private boolean knockedOut;

    @Column(name = "knockout_reasons", length = 1000)
    private String knockoutReasons;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Column(name = "evaluated_by", nullable = false, length = 100)
    private String evaluatedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EvaluationDetailJpaEntity> details = new ArrayList<>();

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EvaluationKnockoutJpaEntity> knockouts = new ArrayList<>();

    // Constructor sin args requerido por JPA
    protected EvaluationJpaEntity() {}

    public EvaluationJpaEntity(UUID id, UUID applicantId, UUID modelId, UUID financialDataId,
            BigDecimal totalScore, String riskLevel, boolean knockedOut, String knockoutReasons,
            OffsetDateTime evaluatedAt, String evaluatedBy,
            OffsetDateTime createdAt, String createdBy) {
        this.id = id;
        this.applicantId = applicantId;
        this.modelId = modelId;
        this.financialDataId = financialDataId;
        this.totalScore = totalScore;
        this.riskLevel = riskLevel;
        this.knockedOut = knockedOut;
        this.knockoutReasons = knockoutReasons;
        this.evaluatedAt = evaluatedAt;
        this.evaluatedBy = evaluatedBy;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    /** Agrega un detalle manteniendo la relación bidireccional. */
    public void addDetail(EvaluationDetailJpaEntity detail) {
        detail.setEvaluation(this);
        this.details.add(detail);
    }

    /** Agrega un knockout manteniendo la relación bidireccional. */
    public void addKnockout(EvaluationKnockoutJpaEntity knockout) {
        knockout.setEvaluation(this);
        this.knockouts.add(knockout);
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getApplicantId() { return applicantId; }
    public UUID getModelId() { return modelId; }
    public UUID getFinancialDataId() { return financialDataId; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getRiskLevel() { return riskLevel; }
    public boolean isKnockedOut() { return knockedOut; }
    public String getKnockoutReasons() { return knockoutReasons; }
    public OffsetDateTime getEvaluatedAt() { return evaluatedAt; }
    public String getEvaluatedBy() { return evaluatedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public List<EvaluationDetailJpaEntity> getDetails() { return details; }
    public List<EvaluationKnockoutJpaEntity> getKnockouts() { return knockouts; }
}
