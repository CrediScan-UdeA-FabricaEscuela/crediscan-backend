package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import co.udea.codefactory.creditscoring.evaluation.domain.model.DecisionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "credit_decision")
public class CreditDecisionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "evaluation_id", nullable = false, unique = true)
    private UUID evaluationId;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision;

    @Column(name = "observations", nullable = false, length = 2000)
    private String observations;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;

    @Column(name = "interest_rate")
    private BigDecimal interestRate;

    @Column(name = "term_months")
    private Integer termMonths;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

    @Column(name = "decided_by", nullable = false)
    private String decidedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    public CreditDecisionJpaEntity() {}
}