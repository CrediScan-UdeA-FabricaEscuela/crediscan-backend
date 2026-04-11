package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest;

import java.net.URI;
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
import org.springframework.data.web.PageableDefault;
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

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;
import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantResult;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.RegisterApplicantUseCase;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.SearchApplicantUseCase;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.UpdateApplicantUseCase;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.ApplicantSearchResponse;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantRequest;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantResponse;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.UpdateApplicantRequest;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.UpdateApplicantResponse;

@RestController
@RequestMapping("/api/v1/solicitantes")
@Tag(name = "Solicitantes", description = "Operaciones sobre solicitantes de crédito")
public class ApplicantController {

    private final RegisterApplicantUseCase registerApplicantUseCase;
    private final SearchApplicantUseCase searchApplicantUseCase;
    private final UpdateApplicantUseCase updateApplicantUseCase;
    private final ApplicantRestMapper applicantRestMapper;

    public ApplicantController(
            RegisterApplicantUseCase registerApplicantUseCase,
            SearchApplicantUseCase searchApplicantUseCase,
            UpdateApplicantUseCase updateApplicantUseCase,
            ApplicantRestMapper applicantRestMapper) {
        this.registerApplicantUseCase = registerApplicantUseCase;
        this.searchApplicantUseCase = searchApplicantUseCase;
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
    @Operation(summary = "Buscar solicitantes",
               description = "Busca por identificación (hash exacto) o nombre (parcial, case-insensitive). Sin q retorna todos.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Búsqueda exitosa"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado",
                content = @Content(schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<Page<ApplicantSearchResponse>> searchApplicants(
            @RequestParam(value = "q", required = false) String criteria,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        PagedResult<ApplicantSummary> results = searchApplicantUseCase.search(
                criteria, new PageRequest(pageable.getPageNumber(), pageable.getPageSize()));
        Page<ApplicantSearchResponse> response = new PageImpl<>(
                results.content().stream().map(applicantRestMapper::toSearchResponse).toList(),
                pageable,
                results.totalElements());
        return ResponseEntity.ok(response);
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
}
