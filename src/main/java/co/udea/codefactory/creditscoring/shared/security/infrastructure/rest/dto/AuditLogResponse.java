package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto;

import java.time.Instant;
import java.util.UUID;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;

public record AuditLogResponse(
        UUID id,
        Instant timestamp,
        String recurso,
        UUID recursoId,
        String accion,
        String usuarioId,
        String ip,
        String resultado,
        String datosAnteriores,
        String datosNuevos,
        String detalles) {

    public static AuditLogResponse from(AuditLogRecord entry) {
        return new AuditLogResponse(
                entry.id(),
                entry.createdAt(),
                entry.entityType(),
                entry.entityId(),
                entry.action(),
                entry.actor(),
                entry.actorIp(),
                entry.result(),
                entry.dataBefore(),
                entry.dataAfter(),
                entry.details());
    }
}
