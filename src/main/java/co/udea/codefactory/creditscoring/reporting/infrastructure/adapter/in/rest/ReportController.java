package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.in.rest;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.reporting.domain.port.in.GetRiskDistributionUseCase;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.GenerarReportePdfPort;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para el reporte de distribución de riesgo.
 * Provee dos endpoints: JSON y PDF. Requiere rol ADMIN o RISK_MANAGER.
 */
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReportController {

    private final GetRiskDistributionUseCase useCase;
    private final GenerarReportePdfPort pdfPort;

    /**
     * Alias mantenido por compatibilidad con el stub anterior (URL con tilde).
     * Delega a la lógica principal. Se conserva para no romper tests de seguridad existentes.
     */
    @GetMapping("/distribución")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER')")
    public ResponseEntity<RiskDistributionReportResponse> jsonLegacy(
            @RequestParam(name = "fecha_desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,

            @RequestParam(name = "fecha_hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,

            @RequestParam(name = "tipo_empleo", required = false) String tipoEmpleo
    ) {
        return json(desde, hasta, tipoEmpleo);
    }

    /**
     * GET /api/v1/reportes/distribucion-riesgo
     * Retorna el reporte de distribución de riesgo en formato JSON.
     */
    @GetMapping("/distribucion-riesgo")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER')")
    public ResponseEntity<RiskDistributionReportResponse> json(
            @RequestParam(name = "fecha_desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,

            @RequestParam(name = "fecha_hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,

            @RequestParam(name = "tipo_empleo", required = false) String tipoEmpleo
    ) {
        EmploymentType emp = parseEmploymentType(tipoEmpleo);
        var report = useCase.report(desde, hasta, emp);
        return ResponseEntity.ok(RiskDistributionReportResponse.from(report));
    }

    /**
     * GET /api/v1/reportes/distribucion-riesgo/pdf
     * Retorna el reporte de distribución de riesgo como documento PDF.
     */
    @GetMapping("/distribucion-riesgo/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER')")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(name = "fecha_desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,

            @RequestParam(name = "fecha_hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,

            @RequestParam(name = "tipo_empleo", required = false) String tipoEmpleo
    ) {
        EmploymentType emp = parseEmploymentType(tipoEmpleo);
        var report = useCase.report(desde, hasta, emp);
        byte[] bytes = pdfPort.generar(report);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=distribucion-riesgo.pdf")
                .body(bytes);
    }

    /**
     * Parsea el tipo de empleo desde el valor de la API.
     * Si es null o vacío retorna null (sin filtro).
     * Si es inválido, {@link EmploymentType#fromApiValue(String)} lanza
     * {@code ApplicantValidationException} que el GlobalExceptionHandler mapea a HTTP 400.
     */
    private EmploymentType parseEmploymentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return EmploymentType.fromApiValue(raw);
    }

    /**
     * Maneja el rango de fechas invertido (fecha_desde > fecha_hasta).
     * El service lanza {@code IllegalArgumentException} — se convierte a HTTP 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(java.util.Map.of(
                        "status", 400,
                        "detail", ex.getMessage(),
                        "errorCode", "INVALID_DATE_RANGE"));
    }
}
