package co.udea.codefactory.creditscoring.shared.security.application.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.GetAuditLogsUseCase;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogQueryPort;

@Service
public class GetAuditLogsService implements GetAuditLogsUseCase {

    static final int MAX_EXPORT_SIZE = 5_000;

    private final AuditLogQueryPort auditLogQueryPort;

    public GetAuditLogsService(AuditLogQueryPort auditLogQueryPort) {
        this.auditLogQueryPort = auditLogQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogRecord> search(AuditLogFilter filter, Pageable pageable) {
        return auditLogQueryPort.search(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogRecord> export(AuditLogFilter filter) {
        Pageable limit = PageRequest.of(0, MAX_EXPORT_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogQueryPort.search(filter, limit).getContent();
    }
}
