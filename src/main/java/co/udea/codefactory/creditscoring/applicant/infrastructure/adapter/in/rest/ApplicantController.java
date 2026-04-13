package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantFilterCriteria;
import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantResult;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.ListApplicantsUseCase;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.RegisterApplicantUseCase;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.UpdateApplicantUseCase;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.ApplicantSearchResponse;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantRequest;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantResponse;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.UpdateApplicantRequest;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.UpdateApplicantResponse;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

@RestController
@RequestMapping("/api/v1/solicitantes")
@Tag(name = "Solicitantes", description = "Operaciones sobre solicitantes de crédito")
public class ApplicantController {

    private final RegisterApplicantUseCase registerApplicantUseCase;
    private final ListApplicantsUseCase listApplicantsUseCase;
    private final UpdateApplicantUseCase updateApplicantUseCase;
    private final ApplicantRestMapper applicantRestMapper;

    public ApplicantController(
            RegisterApplicantUseCase registerApplicantUseCase,
            ListApplicantsUseCase listApplicantsUseCase,
            UpdateApplicantUseCase updateApplicantUseCase,
            ApplicantRestMapper applicantRestMapper) {
        this.registerApplicantUseCase = registerApplicantUseCase;
        this.listApplicantsUseCase = listApplicantsUseCase;
        this.updateApplicantUseCase = updateApplicantUseCase;
        this.applicantRestMapper = applicantRestMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Registrar solicitante", description = "Registra un solicitante de credito")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Solicitante registrado"),
        @ApiResponse(responseCode = "400", description = "Solicitud invalida",
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Identificacion duplicada",
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado",
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<RegisterApplicantResponse> registerApplicant(@Valid @RequestBody RegisterApplicantRequest request) {
        Applicant applicant = registerApplicantUseCase.register(applicantRestMapper.toCommand(request));
        RegisterApplicantResponse response = applicantRestMapper.toResponse(applicant);
        return ResponseEntity
                .created(URI.create("/api/v1/solicitantes/" + applicant.id()))
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ANALYST') or hasRole('RISK_MANAGER') or hasRole('ADMIN') or hasRole('CREDIT_SUPERVISOR')")
    @Operation(summary = "Listar solicitantes con filtros",
               description = "Retorna un listado paginado de solicitantes con filtros combinables. "
                           + "Todos los parámetros son opcionales y se combinan con lógica AND.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Listado retornado exitosamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado",
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<Page<ApplicantSearchResponse>> listApplicants(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "ingresos_min", required = false) BigDecimal incomeMin,
            @RequestParam(value = "ingresos_max", required = false) BigDecimal incomeMax,
            @RequestParam(value = "tipo_empleo", required = false) String employmentType,
            @RequestParam(value = "antiguedad_min", required = false) Integer experienceMin,
            @RequestParam(value = "antiguedad_max", required = false) Integer experienceMax,
            @RequestParam(value = "fecha_registro_desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateFrom,
            @RequestParam(value = "fecha_registro_hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateTo,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        ApplicantFilterCriteria criteria = construirCriterios(
                q, incomeMin, incomeMax, employmentType, experienceMin, experienceMax,
                registrationDateFrom, registrationDateTo, pageable);

        PagedResult<ApplicantSummary> results = listApplicantsUseCase.list(
                criteria, new PageRequest(pageable.getPageNumber(), pageable.getPageSize()));

        Page<ApplicantSearchResponse> response = new PageImpl<>(
                results.content().stream().map(applicantRestMapper::toSearchResponse).toList(),
                pageable,
                results.totalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ANALYST') or hasRole('CREDIT_SUPERVISOR')")
    @Operation(summary = "Exportar solicitantes en CSV",
               description = "Exporta en formato CSV el listado de solicitantes filtrado. "
                           + "Limitado a 10.000 registros. Solo accesible para ANALYST y CREDIT_SUPERVISOR.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Archivo CSV generado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado",
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<String> exportApplicants(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "ingresos_min", required = false) BigDecimal incomeMin,
            @RequestParam(value = "ingresos_max", required = false) BigDecimal incomeMax,
            @RequestParam(value = "tipo_empleo", required = false) String employmentType,
            @RequestParam(value = "antiguedad_min", required = false) Integer experienceMin,
            @RequestParam(value = "antiguedad_max", required = false) Integer experienceMax,
            @RequestParam(value = "fecha_registro_desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateFrom,
            @RequestParam(value = "fecha_registro_hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDateTo) {

        ApplicantFilterCriteria criteria = construirCriterios(
                q, incomeMin, incomeMax, employmentType, experienceMin, experienceMax,
                registrationDateFrom, registrationDateTo, null);

        List<ApplicantSummary> solicitantes = listApplicantsUseCase.export(criteria);
        String csv = construirCsv(solicitantes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=solicitantes.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Editar solicitante",
               description = "Actualiza campos permitidos. La identificación no puede modificarse.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitante actualizado"),
        @ApiResponse(responseCode = "400", description = "Campo inmutable o validación fallida",
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Solicitante no encontrado",
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado",
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<UpdateApplicantResponse> updateApplicant(
            @PathVariable("id") UUID id,
            @RequestBody UpdateApplicantRequest request,
            Authentication authentication) {
        UpdateApplicantResult result = updateApplicantUseCase.update(
                applicantRestMapper.toUpdateCommand(id, request, authentication.getName()));
        return ResponseEntity.ok(applicantRestMapper.toUpdateResponse(result));
    }

    /**
     * Ensambla el objeto de criterios a partir de los parámetros individuales del request.
     * Extrae el campo y dirección de ordenamiento del Pageable de Spring cuando está disponible.
     */
    private ApplicantFilterCriteria construirCriterios(
            String q, BigDecimal incomeMin, BigDecimal incomeMax, String employmentType,
            Integer experienceMin, Integer experienceMax,
            LocalDate registrationDateFrom, LocalDate registrationDateTo,
            Pageable pageable) {

        String campoCriteria = null;
        String direccionCriteria = null;

        if (pageable != null && pageable.getSort().isSorted()) {
            Sort.Order orden = pageable.getSort().iterator().next();
            campoCriteria = orden.getProperty();
            direccionCriteria = orden.getDirection().name();
        }

        return new ApplicantFilterCriteria(
                q, incomeMin, incomeMax, employmentType,
                experienceMin, experienceMax,
                registrationDateFrom, registrationDateTo,
                campoCriteria, direccionCriteria);
    }

    /**
     * Construye el contenido CSV a partir de la lista de solicitantes.
     * Sigue el mismo patrón de {@code AuditLogController.buildCsv()}.
     */
    private String construirCsv(List<ApplicantSummary> solicitantes) {
        String encabezado = "nombre,identificacion,fecha_nacimiento,tipo_empleo,"
                + "ingresos_mensuales,antiguedad_laboral,telefono,direccion,correo_electronico";
        StringBuilder csv = new StringBuilder(encabezado);
        for (ApplicantSummary s : solicitantes) {
            csv.append('\n')
               .append(escaparCsv(s.name())).append(',')
               .append(escaparCsv(s.identification())).append(',')
               .append(s.birthDate() != null ? s.birthDate().toString() : "").append(',')
               .append(escaparCsv(s.employmentType())).append(',')
               .append(s.monthlyIncome() != null ? s.monthlyIncome().toPlainString() : "").append(',')
               .append(s.workExperienceMonths() != null ? s.workExperienceMonths().toString() : "").append(',')
               .append(escaparCsv(s.phone())).append(',')
               .append(escaparCsv(s.address())).append(',')
               .append(escaparCsv(s.email()));
        }
        return csv.toString();
    }

    private String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }
        String escapado = valor.replace("\"", "\"\"");
        if (escapado.contains(",") || escapado.contains("\n") || escapado.contains("\"") || escapado.contains("\r")) {
            return '"' + escapado + '"';
        }
        return escapado;
    }
}
