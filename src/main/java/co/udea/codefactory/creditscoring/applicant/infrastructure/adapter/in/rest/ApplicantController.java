package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.RegisterApplicantUseCase;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantRequest;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantResponse;

@RestController
@RequestMapping("/api/v1/solicitantes")
@Tag(name = "Solicitantes", description = "Operaciones para registro de solicitantes")
public class ApplicantController {

    private final RegisterApplicantUseCase registerApplicantUseCase;
    private final ApplicantRestMapper applicantRestMapper;

    public ApplicantController(
            RegisterApplicantUseCase registerApplicantUseCase,
            ApplicantRestMapper applicantRestMapper) {
        this.registerApplicantUseCase = registerApplicantUseCase;
        this.applicantRestMapper = applicantRestMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ANALISTA_CREDITO')")
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
}
