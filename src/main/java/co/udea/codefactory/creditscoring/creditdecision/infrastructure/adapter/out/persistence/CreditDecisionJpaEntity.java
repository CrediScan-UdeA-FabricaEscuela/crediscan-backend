package co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.CreatedBy;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.DecisionStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA para la tabla credit_decision.
 * Mapea la decisión crediticia a la base de datos.
 */
@Entity
@Table(name = "credit_decision")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditDecisionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "evaluation_id", nullable = false, unique = true)
    private UUID evaluationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private DecisionStatus decision;

    @Column(name = "observations", nullable = false, length = 2000)
    private String observations;

    @Column(name = "decided_by", nullable = false, length = 100)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "supervisor_id", length = 100)
    private String supervisorId;

    @Column(name = "resolution_deadline_at")
    private OffsetDateTime resolutionDeadlineAt;

    // Campos ignorados en esta HU: approved_amount, interest_rate, term_months
    // (corresponden a HUs futuras de aprobación con condiciones)
}
