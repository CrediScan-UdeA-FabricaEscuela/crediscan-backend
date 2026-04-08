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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
        Page<AuditLogRecord> page = new PageImpl<>(List.of(record));
        when(auditLogQueryPort.search(any(AuditLogFilter.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        Page<AuditLogRecord> result = service.search(new AuditLogFilter(null, null, null, null, null, null, null), PageRequest.of(0, 50));

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(auditLogQueryPort).search(any(AuditLogFilter.class), any(org.springframework.data.domain.Pageable.class));
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
        Page<AuditLogRecord> page = new PageImpl<>(List.of(record));
        when(auditLogQueryPort.search(any(AuditLogFilter.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        List<AuditLogRecord> result = service.export(new AuditLogFilter(null, null, null, null, null, null, null));

        assertThat(result).hasSize(1);
        verify(auditLogQueryPort).search(
                any(AuditLogFilter.class),
                org.mockito.ArgumentMatchers.argThat(p -> p.getPageSize() == GetAuditLogsService.MAX_EXPORT_SIZE));
    }
}
