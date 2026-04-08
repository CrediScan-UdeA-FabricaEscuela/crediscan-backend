package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto;

import java.time.Instant;
import java.util.UUID;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;

public class AuditLogResponse {

    private final UUID id;
    private final Instant timestamp;
    private final String recurso;
    private final UUID recursoId;
    private final String accion;
    private final String usuarioId;
    private final String ip;
    private final String resultado;
    private final String datosAnteriores;
    private final String datosNuevos;
    private final String detalles;

    public AuditLogResponse(UUID id,
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
        this.id = id;
        this.timestamp = timestamp;
        this.recurso = recurso;
        this.recursoId = recursoId;
        this.accion = accion;
        this.usuarioId = usuarioId;
        this.ip = ip;
        this.resultado = resultado;
        this.datosAnteriores = datosAnteriores;
        this.datosNuevos = datosNuevos;
        this.detalles = detalles;
    }

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

    public UUID getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getRecurso() {
        return recurso;
    }

    public UUID getRecursoId() {
        return recursoId;
    }

    public String getAccion() {
        return accion;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getIp() {
        return ip;
    }

    public String getResultado() {
        return resultado;
    }

    public String getDatosAnteriores() {
        return datosAnteriores;
    }

    public String getDatosNuevos() {
        return datosNuevos;
    }

    public String getDetalles() {
        return detalles;
    }
}
