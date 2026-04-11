package co.udea.codefactory.creditscoring.shared.security.domain.port.out;

import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;

// Puerto de salida para consulta paginada de logs de auditoría sin acoplamiento a Spring
public interface AuditLogQueryPort {

    PagedResult<AuditLogRecord> search(AuditLogFilter filter, PageRequest pageRequest);
}
