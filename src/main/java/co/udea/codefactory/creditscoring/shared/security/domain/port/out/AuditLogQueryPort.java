package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;

public interface AuditLogQueryPort {

    Page<AuditLogRecord> search(AuditLogFilter filter, Pageable pageable);
}
