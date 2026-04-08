package co.udea.codefactory.creditscoring.shared.security.infrastructure.rest;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogFilter;
import co.udea.codefactory.creditscoring.shared.security.domain.model.AuditLogRecord;
import co.udea.codefactory.creditscoring.shared.security.domain.port.in.GetAuditLogsUseCase;
import co.udea.codefactory.creditscoring.shared.security.infrastructure.rest.dto.AuditLogResponse;

@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoría", description = "Consultas y exportación de logs de auditoría")
public class AuditLogController {

    private static final int MAX_PAGE_SIZE = 50;

    private final GetAuditLogsUseCase getAuditLogsUseCase;

    public AuditLogController(GetAuditLogsUseCase getAuditLogsUseCase) {
        this.getAuditLogsUseCase = getAuditLogsUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER','ANALYST')")
    @Operation(summary = "Buscar logs de auditoría",
               description = "Filtra y pagina la consulta de auditoría. Los analistas solo pueden ver su propia actividad.")
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @RequestParam(value = "fecha_desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "fecha_hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "usuario_id", required = false) String actor,
            @RequestParam(value = "accion", required = false) String action,
            @RequestParam(value = "recurso", required = false) String entityType,
            @RequestParam(value = "recurso_id", required = false) String entityIdValue,
            @RequestParam(value = "ip", required = false) String actorIp,
            @PageableDefault(size = MAX_PAGE_SIZE, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        String effectiveActor = enforceActorScope(authentication, actor);
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE), pageable.getSort());

        AuditLogFilter filter = new AuditLogFilter(
                from,
                to,
                effectiveActor,
                action,
                entityType,
                parseUuid(entityIdValue),
                actorIp);

        Page<AuditLogRecord> result = getAuditLogsUseCase.search(filter, pageRequest);
        Page<AuditLogResponse> response = result.map(AuditLogResponse::from);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER','ANALYST')")
    @Operation(summary = "Exportar logs de auditoría",
               description = "Exporta en CSV la consulta de auditoría. Los analistas solo pueden exportar su propia actividad.")
    public ResponseEntity<String> exportAuditLogs(
            @RequestParam(value = "fecha_desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "fecha_hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "usuario_id", required = false) String actor,
            @RequestParam(value = "accion", required = false) String action,
            @RequestParam(value = "recurso", required = false) String entityType,
            @RequestParam(value = "recurso_id", required = false) String entityIdValue,
            @RequestParam(value = "ip", required = false) String actorIp,
            Authentication authentication) {

        String effectiveActor = enforceActorScope(authentication, actor);

        AuditLogFilter filter = new AuditLogFilter(
                from,
                to,
                effectiveActor,
                action,
                entityType,
                parseUuid(entityIdValue),
                actorIp);

        List<AuditLogRecord> records = getAuditLogsUseCase.export(filter);
        String csv = buildCsv(records);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=auditoria.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    private String enforceActorScope(Authentication authentication, String requestedActor) {
        boolean isAnalyst = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ANALYST"));
        if (isAnalyst) {
            return authentication.getName();
        }
        return requestedActor;
    }

    private String buildCsv(List<AuditLogRecord> records) {
        String header = "timestamp,usuario_id,accion,recurso,recurso_id,datos_anteriores,datos_nuevos,ip,resultado,detalles";
        StringBuilder csv = new StringBuilder(header);
        for (AuditLogRecord record : records) {
            csv.append('\n')
               .append(escapeCsv(record.createdAt().toString())).append(',')
               .append(escapeCsv(record.actor())).append(',')
               .append(escapeCsv(record.action())).append(',')
               .append(escapeCsv(record.entityType())).append(',')
               .append(record.entityId() != null ? escapeCsv(record.entityId().toString()) : "").append(',')
               .append(escapeCsv(record.dataBefore())).append(',')
               .append(escapeCsv(record.dataAfter())).append(',')
               .append(escapeCsv(record.actorIp())).append(',')
               .append(escapeCsv(record.result())).append(',')
               .append(escapeCsv(record.details()));
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"") || escaped.contains("\r")) {
            return '"' + escaped + '"';
        }
        return escaped;
    }

    private java.util.UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.util.UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
