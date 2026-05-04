package co.udea.codefactory.creditscoring.creditdecision.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Agregado raíz del contexto de decisiones crediticias.
 *
 * <p>Representa la decisión final tomada sobre una evaluación crediticia.
 * Las decisiones son inmutables una vez registradas (CA6, RN2).</p>
 *
 * <p>Reglas de negocio:
 * <ul>
 *   <li>CA1: Solo una decisión por evaluación</li>
 *   <li>CA2: Las opciones son APPROVED, REJECTED, MANUAL_REVIEW, ESCALATED</li>
 *   <li>CA3: Observaciones obligatorias con mínimo 20 caracteres</li>
 *   <li>CA4/RN1: Si la evaluación fue rechazada por knock-out, solo REJECTED</li>
 *   <li>CA5: Se registra evaluación_id, decisión, observaciones, analista, fecha</li>
 *   <li>CA7/RN4: Decisiones ESCALATED tienen deadline de 48h y requieren supervisor</li>
 * </ul>
 */
public record CreditDecision(
        UUID id,
        UUID evaluationId,
        DecisionStatus decision,
        String observations,
        String analystId,
        OffsetDateTime decidedAt,
        OffsetDateTime createdAt,
        String createdBy,
        String supervisorId,
        OffsetDateTime resolutionDeadlineAt
) {

    private static final long ESCALATION_DEADLINE_HOURS = 48;

    public CreditDecision {
        if (id == null) throw new IllegalArgumentException("El id de la decisión es obligatorio");
        if (evaluationId == null) throw new IllegalArgumentException("El evaluationId es obligatorio");
        if (decision == null) throw new IllegalArgumentException("El estado de la decisión es obligatorio");
        if (observations == null || observations.length() < 20) {
            throw new IllegalArgumentException("Las observaciones deben tener al menos 20 caracteres");
        }
        if (analystId == null || analystId.isBlank())
            throw new IllegalArgumentException("El analista es obligatorio");
        if (decidedAt == null) throw new IllegalArgumentException("La fecha de decisión es obligatoria");
        if (createdAt == null) throw new IllegalArgumentException("La fecha de creación es obligatoria");
        if (createdBy == null || createdBy.isBlank())
            throw new IllegalArgumentException("El creador es obligatorio");
    }

    /**
     * Factory method para crear una nueva decisión crediticia.
     * Para decisiones ESCALATED, calcula automáticamente el deadline de 48h (RN4).
     */
    public static CreditDecision crear(UUID evaluationId, DecisionStatus decision,
                                        String observations, String analystId) {
        OffsetDateTime ahora = OffsetDateTime.now();
        OffsetDateTime deadline = decision == DecisionStatus.ESCALATED
                ? ahora.plusHours(ESCALATION_DEADLINE_HOURS)
                : null;
        return new CreditDecision(
                UUID.randomUUID(),
                evaluationId,
                decision,
                observations,
                analystId,
                ahora,
                ahora,
                analystId,
                null,
                deadline
        );
    }

    /**
     * Factory method para reconstruir una decisión desde persistencia.
     */
    public static CreditDecision rehydrate(UUID id, UUID evaluationId, DecisionStatus decision,
                                            String observations, String analystId,
                                            OffsetDateTime decidedAt,
                                            OffsetDateTime createdAt, String createdBy,
                                            String supervisorId,
                                            OffsetDateTime resolutionDeadlineAt) {
        return new CreditDecision(
                id, evaluationId, decision, observations,
                analystId, decidedAt, createdAt, createdBy,
                supervisorId, resolutionDeadlineAt
        );
    }
}
