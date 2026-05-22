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
import co.udea.codefactory.creditscoring.reporting.domain.port.in.efectividad.GetEfectividadModeloUseCase;
import co.udea.codefactory.creditscoring.reporting.domain.port.in.analistas.GetActividadAnalistasUseCase;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.GenerarReportePdfPort;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.efectividad.GenerarEfectividadPdfPort;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.GenerarActividadAnalistasPdfPort;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.GenerarActividadAnalistasCSVPort;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para los reportes analíticos de crédito.
 * <p>
 * Agrupa los endpoints de distribución de riesgo (HU-015),
 * efectividad del modelo (HU-016) y actividad de analistas (HU-017).
 * </p>
 */
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReportController {

    private final GetRiskDistributionUseCase useCase;
    private final GenerarReportePdfPort pdfPort;
    private final GetEfectividadModeloUseCase efectividadUseCase;
    private final GenerarEfectividadPdfPort efectividadPdfPort;
    private final GetActividadAnalistasUseCase actividadUseCase;
    private final GenerarActividadAnalistasPdfPort actividadPdfPort;
    private final GenerarActividadAnalistasCSVPort actividadCsvPort;

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

    // =========================================================================
    // HU-016 — Efectividad del Modelo
    // =========================================================================

    /**
     * GET /api/v1/reportes/efectividad-modelo
     * Retorna el reporte de efectividad del modelo en formato JSON.
     * Roles permitidos: ADMIN, CREDIT_SUPERVISOR, RISK_MANAGER.
     */
    @GetMapping("/efectividad-modelo")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<EfectividadModeloReportResponse> efectividadJson(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,

            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,

            @RequestParam(name = "analistaId", required = false) String analistaId
    ) {
        validarParametros(desde, hasta);
        var reporte = efectividadUseCase.reporte(desde, hasta, analistaId);
        return ResponseEntity.ok(EfectividadModeloReportResponse.from(reporte));
    }

    /**
     * GET /api/v1/reportes/efectividad-modelo/pdf
     * Retorna el reporte de efectividad del modelo como PDF.
     */
    @GetMapping("/efectividad-modelo/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<byte[]> efectividadPdf(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,

            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,

            @RequestParam(name = "analistaId", required = false) String analistaId
    ) {
        validarParametros(desde, hasta);
        var reporte = efectividadUseCase.reporte(desde, hasta, analistaId);
        byte[] bytes = efectividadPdfPort.generar(reporte);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=efectividad-modelo.pdf")
                .body(bytes);
    }

    // =========================================================================
    // HU-017 — Actividad de Analistas
    // =========================================================================

    /**
     * GET /api/v1/reportes/actividad-analistas
     * Retorna el reporte de actividad de analistas en formato JSON.
     * Roles permitidos: ADMIN, CREDIT_SUPERVISOR (RISK_MANAGER denegado — HU-017).
     */
    @GetMapping("/actividad-analistas")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_SUPERVISOR')")
    public ResponseEntity<ActividadAnalistasReportResponse> actividadJson(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,

            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta
    ) {
        validarParametros(desde, hasta);
        var reporte = actividadUseCase.reporte(desde, hasta);
        return ResponseEntity.ok(ActividadAnalistasReportResponse.from(reporte));
    }

    /**
     * GET /api/v1/reportes/actividad-analistas/pdf
     * Retorna el reporte de actividad de analistas como PDF.
     */
    @GetMapping("/actividad-analistas/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_SUPERVISOR')")
    public ResponseEntity<byte[]> actividadPdf(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,

            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta
    ) {
        validarParametros(desde, hasta);
        var reporte = actividadUseCase.reporte(desde, hasta);
        byte[] bytes = actividadPdfPort.generar(reporte);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=actividad-analistas.pdf")
                .body(bytes);
    }

    /**
     * GET /api/v1/reportes/actividad-analistas/csv
     * Retorna el reporte de actividad de analistas en formato CSV.
     */
    @GetMapping("/actividad-analistas/csv")
    @PreAuthorize("hasAnyRole('ADMIN','CREDIT_SUPERVISOR')")
    public ResponseEntity<byte[]> actividadCsv(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,

            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta
    ) {
        validarParametros(desde, hasta);
        var reporte = actividadUseCase.reporte(desde, hasta);
        byte[] bytes = actividadCsvPort.generar(reporte);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=actividad-analistas.csv")
                .body(bytes);
    }

    // =========================================================================
    // Helpers compartidos
    // =========================================================================

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
     * Valida que ambos parámetros de fecha estén presentes.
     * Si alguno es null lanza {@link IllegalArgumentException} que se mapea a HTTP 400.
     */
    private void validarParametros(OffsetDateTime desde, OffsetDateTime hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException(
                    "Los parámetros 'desde' y 'hasta' son obligatorios");
        }
    }

    /**
     * Maneja el rango de fechas invertido (desde > hasta) y los parámetros faltantes.
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
