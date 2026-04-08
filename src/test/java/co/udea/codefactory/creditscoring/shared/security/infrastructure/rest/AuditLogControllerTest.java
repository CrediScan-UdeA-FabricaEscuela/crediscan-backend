package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.GetAuditLogsUseCase;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto.AuditLogResponse;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private GetAuditLogsUseCase getAuditLogsUseCase;

    private AuditLogController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditLogController(getAuditLogsUseCase);
    }

    @Test
    void searchAuditLogs_asAdmin_returnsPagedResults() {
        AuditLogRecord record = new AuditLogRecord(
                UUID.randomUUID(),
                Instant.parse("2026-04-07T12:00:00Z"),
                "USER",
                UUID.randomUUID(),
                "CREATE",
                "admin",
                "127.0.0.1",
                "SUCCESS",
                "{}",
                "{\"username\":\"admin\"}",
                null);
        Page<AuditLogRecord> page = new PageImpl<>(List.of(record), PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(getAuditLogsUseCase.search(any(AuditLogFilter.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        Authentication auth = new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Page<AuditLogResponse> response = controller.searchAuditLogs(
                Instant.parse("2026-04-07T00:00:00Z"),
                Instant.parse("2026-04-08T00:00:00Z"),
                null,
                "CREATE",
                "USER",
                null,
                null,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")),
                auth).getBody();

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).accion()).isEqualTo("CREATE");
        verify(getAuditLogsUseCase).search(any(AuditLogFilter.class), any(PageRequest.class));
    }

    @Test
    void exportAuditLogs_asAnalyst_forcesActorScope() {
        AuditLogRecord record = new AuditLogRecord(
                UUID.randomUUID(),
                Instant.parse("2026-04-07T12:00:00Z"),
                "USER",
                UUID.randomUUID(),
                "READ",
                "analyst",
                "127.0.0.1",
                "SUCCESS",
                null,
                null,
                null);
        when(getAuditLogsUseCase.export(any(AuditLogFilter.class))).thenReturn(List.of(record));

        Authentication auth = new UsernamePasswordAuthenticationToken("analyst", null,
                List.of(new SimpleGrantedAuthority("ROLE_ANALYST")));
        String csv = controller.exportAuditLogs(
                null,
                null,
                "admin",
                null,
                null,
                null,
                null,
                auth).getBody();

        assertThat(csv).startsWith("timestamp,usuario_id,accion,recurso,recurso_id,datos_anteriores,datos_nuevos,ip,resultado,detalles");
        assertThat(csv).contains("analyst");

        ArgumentCaptor<AuditLogFilter> filterCaptor = ArgumentCaptor.forClass(AuditLogFilter.class);
        verify(getAuditLogsUseCase).export(filterCaptor.capture());
        assertThat(filterCaptor.getValue().actor()).isEqualTo("analyst");
    }
}
