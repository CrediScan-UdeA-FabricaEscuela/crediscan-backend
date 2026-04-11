package co.udea.codefactory.creditscoring.shared.security.domain.port.in;

import java.util.List;

import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;

// Puerto de entrada para consulta y exportación de logs de auditoría
public interface GetAuditLogsUseCase {

    PagedResult<AuditLogRecord> search(AuditLogFilter filter, PageRequest pageRequest);

    List<AuditLogRecord> export(AuditLogFilter filter);
}
