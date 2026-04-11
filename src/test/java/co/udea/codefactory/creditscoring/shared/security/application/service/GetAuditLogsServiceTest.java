package co.udea.codefactory.creditscoring.shared.security.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;
import co.udea.codefactory.creditscoring.shared.security.domain.port.out.AuditLogQueryPort;

@ExtendWith(MockitoExtension.class)
class GetAuditLogsServiceTest {

    @Mock
    private AuditLogQueryPort auditLogQueryPort;

    private GetAuditLogsService service;

    @BeforeEach
    void setUp() {
        service = new GetAuditLogsService(auditLogQueryPort);
    }

    @Test
    void search_delegatesToQueryPort() {
        AuditLogRecord record = new AuditLogRecord(
                UUID.randomUUID(),
                Instant.parse("2026-04-07T12:00:00Z"),
                "USER",
                UUID.randomUUID(),
                "CREATE",
                "admin",
                "127.0.0.1",
                "SUCCESS",
                null,
                null,
                null);
        // Resultado paginado usando tipos de dominio en lugar de Spring Page
        PagedResult<AuditLogRecord> pagedResult = new PagedResult<>(List.of(record), 1L, 1, 0, 50);
        when(auditLogQueryPort.search(any(AuditLogFilter.class), any(PageRequest.class))).thenReturn(pagedResult);

        PagedResult<AuditLogRecord> result = service.search(
                new AuditLogFilter(null, null, null, null, null, null, null),
                new PageRequest(0, 50));

        assertThat(result.totalElements()).isEqualTo(1);
        verify(auditLogQueryPort).search(any(AuditLogFilter.class), any(PageRequest.class));
    }

    @Test
    void export_delegatesToQueryPortWithSizeLimit() {
        AuditLogRecord record = new AuditLogRecord(
                UUID.randomUUID(),
                Instant.parse("2026-04-07T12:00:00Z"),
                "USER",
                UUID.randomUUID(),
                "READ",
                "admin",
                "127.0.0.1",
                "SUCCESS",
                null,
                null,
                null);
        PagedResult<AuditLogRecord> pagedResult = new PagedResult<>(List.of(record), 1L, 1, 0, GetAuditLogsService.MAX_EXPORT_SIZE);
        when(auditLogQueryPort.search(any(AuditLogFilter.class), any(PageRequest.class))).thenReturn(pagedResult);

        List<AuditLogRecord> result = service.export(new AuditLogFilter(null, null, null, null, null, null, null));

        assertThat(result).hasSize(1);
        // Verifica que se usó el límite máximo de exportación
        verify(auditLogQueryPort).search(
                any(AuditLogFilter.class),
                org.mockito.ArgumentMatchers.argThat(p -> p.size() == GetAuditLogsService.MAX_EXPORT_SIZE));
    }
}
