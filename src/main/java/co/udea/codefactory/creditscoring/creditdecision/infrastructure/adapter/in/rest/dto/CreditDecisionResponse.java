package co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.in.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de respuesta que representa una decisión crediticia registrada.
 */
public record CreditDecisionResponse(
        UUID id,
        UUID evaluationId,
        String decision,
        String observations,
        String decidedBy,
        OffsetDateTime decidedAt,
        OffsetDateTime createdAt,
        String supervisorId,
        OffsetDateTime resolutionDeadlineAt
) {}
