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

    public static AuditLogResponse from(AuditLogRecord record) {
        return new AuditLogResponse(
                record.id(),
                record.createdAt(),
                record.entityType(),
                record.entityId(),
                record.action(),
                record.actor(),
                record.actorIp(),
                record.result(),
                record.dataBefore(),
                record.dataAfter(),
                record.details());
    }
}
