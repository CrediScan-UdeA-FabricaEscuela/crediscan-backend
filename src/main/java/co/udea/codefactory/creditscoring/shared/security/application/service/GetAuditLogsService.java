package co.udea.codefactory.creditscoring.shared.security.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.GetAuditLogsUseCase;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogQueryPort;

// Servicio de aplicación para búsqueda y exportación de logs de auditoría
@Service
public class GetAuditLogsService implements GetAuditLogsUseCase {

    static final int MAX_EXPORT_SIZE = 5_000;

    private final AuditLogQueryPort auditLogQueryPort;

    public GetAuditLogsService(AuditLogQueryPort auditLogQueryPort) {
        this.auditLogQueryPort = auditLogQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<AuditLogRecord> search(AuditLogFilter filter, PageRequest pageRequest) {
        return auditLogQueryPort.search(filter, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogRecord> export(AuditLogFilter filter) {
        // Limita la exportación a MAX_EXPORT_SIZE registros más recientes
        PageRequest limit = new PageRequest(0, MAX_EXPORT_SIZE);
        return auditLogQueryPort.search(filter, limit).content();
    }
}
