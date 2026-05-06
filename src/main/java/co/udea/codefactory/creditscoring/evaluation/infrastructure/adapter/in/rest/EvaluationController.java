package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.CreditDecisionRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.application.dto.ExecuteEvaluationCommand;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetail;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationKnockout;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.ExecuteEvaluationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationReportUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.EvaluationJpaEntity;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.EvaluationPersistenceMapper;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.JpaEvaluationRepository;

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
    private final CreditDecisionRepositoryPort creditDecisionRepository;
    private final EvaluationRepositoryPort evaluationDomainRepository;
    private final EvaluationPersistenceMapper evaluationMapper;
    private final JpaEvaluationRepository jpaEvaluationRepository;

    public EvaluationController(
            ExecuteEvaluationUseCase executeEvaluationUseCase,
            GetEvaluationUseCase getEvaluationUseCase,
            GetEvaluationReportUseCase getEvaluationReportUseCase,
            CreditDecisionRepositoryPort creditDecisionRepository,
            EvaluationRepositoryPort evaluationDomainRepository,
            EvaluationPersistenceMapper evaluationMapper,
            JpaEvaluationRepository jpaEvaluationRepository) {
        this.executeEvaluationUseCase = executeEvaluationUseCase;
        this.getEvaluationUseCase = getEvaluationUseCase;
        this.getEvaluationReportUseCase = getEvaluationReportUseCase;
        this.creditDecisionRepository = creditDecisionRepository;
        this.evaluationDomainRepository = evaluationDomainRepository;
        this.evaluationMapper = evaluationMapper;
        this.jpaEvaluationRepository = jpaEvaluationRepository;
    }

    /** Lista evaluaciones con su desglose completo. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EvaluationResponse>> listEvaluaciones() {
        List<EvaluationJpaEntity> entities = jpaEvaluationRepository.findAllByOrderByEvaluatedAtDesc();
        return ResponseEntity.ok(entities.stream()
                .map(evaluationMapper::toDomain)
                .map(this::toResponse)
                .toList());
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
                e.evaluatedAt(), e.evaluatedBy(), details, knockouts,
                creditDecisionRepository.existsByEvaluationId(e.id()));
    }
}
