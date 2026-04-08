package co.udea.codefactory.creditscoring.shared.security.domain.port.in;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;

public interface GetAuditLogsUseCase {

    Page<AuditLogRecord> search(AuditLogFilter filter, Pageable pageable);

    List<AuditLogRecord> export(AuditLogFilter filter);
}
