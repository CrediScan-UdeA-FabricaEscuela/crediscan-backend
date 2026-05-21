package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

public interface AuditLogPort {

    void registrar(AuditLogEntry entry);
}
