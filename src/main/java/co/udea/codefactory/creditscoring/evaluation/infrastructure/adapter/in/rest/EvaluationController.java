package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.evaluation.application.dto.ExecuteEvaluationCommand;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetail;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationKnockout;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.ExecuteEvaluationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationReportUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationUseCase;

/**
 * Controlador REST para el bounded context de evaluaciones crediticias.
 * Expone los endpoints para ejecutar evaluaciones, consultarlas y descargar su reporte PDF.
 */
@RestController
@RequestMapping("/api/v1/evaluaciones")
public class EvaluationController {

    private final ExecuteEvaluationUseCase executeEvaluationUseCase;
    private final GetEvaluationUseCase getEvaluationUseCase;
    private final GetEvaluationReportUseCase getEvaluationReportUseCase;

    public EvaluationController(
            ExecuteEvaluationUseCase executeEvaluationUseCase,
            GetEvaluationUseCase getEvaluationUseCase,
            GetEvaluationReportUseCase getEvaluationReportUseCase) {
        this.executeEvaluationUseCase = executeEvaluationUseCase;
        this.getEvaluationUseCase = getEvaluationUseCase;
        this.getEvaluationReportUseCase = getEvaluationReportUseCase;
    }

    /** Lista evaluaciones — stub sin lógica de negocio (fuera del scope de HU-010). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<List<Object>> listEvaluaciones() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    /** Ejecuta una nueva evaluación crediticia para un solicitante. Retorna 201 con Location. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ResponseEntity<EvaluationResponse> ejecutarEvaluacion(
            @Valid @RequestBody ExecuteEvaluationRequest request) {
        Evaluation evaluation = executeEvaluationUseCase.ejecutar(
                new ExecuteEvaluationCommand(request.applicantId(), request.modelId()));
        return ResponseEntity
                .created(URI.create("/api/v1/evaluaciones/" + evaluation.id()))
                .body(toResponse(evaluation));
    }

    /** Consulta una evaluación por su identificador. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<EvaluationResponse> obtenerEvaluacion(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(getEvaluationUseCase.obtenerPorId(id)));
    }

    /** Descarga el reporte PDF de una evaluación. */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID id) {
        byte[] pdf = getEvaluationReportUseCase.generarReporte(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=evaluacion-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    // Mapeo de dominio a DTO de respuesta
    private EvaluationResponse toResponse(Evaluation e) {
        List<EvaluationResponse.DetailDto> details = e.details().stream()
                .map(d -> new EvaluationResponse.DetailDto(
                        d.id(), d.variableId(), d.variableName(),
                        d.rawValue(), d.score(), d.weight(), d.weightedScore()))
                .toList();

        List<EvaluationResponse.KnockoutDto> knockouts = e.knockouts().stream()
                .map(k -> new EvaluationResponse.KnockoutDto(
                        k.id(), k.ruleId(), k.ruleName(), k.fieldValue(), k.triggered()))
                .toList();

        return new EvaluationResponse(
                e.id(), e.applicantId(), e.modelId(), e.financialDataId(),
                e.totalScore(), e.riskLevel().name(), e.knockedOut(), e.knockoutReasons(),
                e.evaluatedAt(), e.evaluatedBy(), details, knockouts);
    }
}
