package co.udea.codefactory.creditscoring.evaluation.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class CreditDecision {

    private final UUID id;
    private final UUID evaluationId;
    private final DecisionType decision;
    private final String observations;

    private final BigDecimal approvedAmount;
    private final BigDecimal interestRate;
    private final Integer termMonths;

    private final OffsetDateTime decidedAt;
    private final String decidedBy;

    private final OffsetDateTime createdAt;
    private final String createdBy;

    public CreditDecision(
            UUID id,
            UUID evaluationId,
            DecisionType decision,
            String observations,
            BigDecimal approvedAmount,
            BigDecimal interestRate,
            Integer termMonths,
            OffsetDateTime decidedAt,
            String decidedBy,
            OffsetDateTime createdAt,
            String createdBy) {

        if (evaluationId == null) {
            throw new IllegalArgumentException("evaluationId obligatorio");
        }

        if (decision == null) {
            throw new IllegalArgumentException("decision obligatoria");
        }

        if (observations == null || observations.trim().length() < 20) {
            throw new IllegalArgumentException("observations debe tener mínimo 20 caracteres");
        }

        if (decidedBy == null || decidedBy.isBlank()) {
            throw new IllegalArgumentException("decidedBy obligatorio");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.evaluationId = evaluationId;
        this.decision = decision;
        this.observations = observations;

        this.approvedAmount = approvedAmount;
        this.interestRate = interestRate;
        this.termMonths = termMonths;

        this.decidedAt = decidedAt != null ? decidedAt : OffsetDateTime.now();
        this.decidedBy = decidedBy;

        this.createdAt = createdAt != null ? createdAt : OffsetDateTime.now();
        this.createdBy = createdBy != null ? createdBy : decidedBy;
    }
}