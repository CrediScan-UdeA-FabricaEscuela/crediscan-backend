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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA para la tabla evaluation.
 * Es el agregado raíz persistido y contiene las relaciones con detail y knockout.
 */
@Entity
@Table(name = "evaluation")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Builder.Default
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EvaluationDetailJpaEntity> details = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EvaluationKnockoutJpaEntity> knockouts = new ArrayList<>();

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
}
