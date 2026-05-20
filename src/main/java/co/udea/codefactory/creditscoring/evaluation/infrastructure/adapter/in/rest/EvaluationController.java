package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.CreditDecisionRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.application.dto.ExecuteEvaluationCommand;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ClassificationItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationComparison;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetail;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetailView;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationKnockout;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.CompareEvaluationsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.ExecuteEvaluationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetClassificationByLevelUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationClassificationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationDetailUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationReportUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.ClassificationItemResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.ClassificationSummaryResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.EvaluationComparisonResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.EvaluationDetailResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.LevelCountDto;
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
    private final GetEvaluationClassificationUseCase getEvaluationClassificationUseCase;
    private final GetClassificationByLevelUseCase getClassificationByLevelUseCase;
    private final GetEvaluationDetailUseCase getEvaluationDetailUseCase;
    private final CompareEvaluationsUseCase compareEvaluationsUseCase;

    public EvaluationController(
            ExecuteEvaluationUseCase executeEvaluationUseCase,
            GetEvaluationUseCase getEvaluationUseCase,
            GetEvaluationReportUseCase getEvaluationReportUseCase,
            CreditDecisionRepositoryPort creditDecisionRepository,
            EvaluationRepositoryPort evaluationDomainRepository,
            EvaluationPersistenceMapper evaluationMapper,
            JpaEvaluationRepository jpaEvaluationRepository,
            GetEvaluationClassificationUseCase getEvaluationClassificationUseCase,
            GetClassificationByLevelUseCase getClassificationByLevelUseCase,
            GetEvaluationDetailUseCase getEvaluationDetailUseCase,
            CompareEvaluationsUseCase compareEvaluationsUseCase) {
        this.executeEvaluationUseCase = executeEvaluationUseCase;
        this.getEvaluationUseCase = getEvaluationUseCase;
        this.getEvaluationReportUseCase = getEvaluationReportUseCase;
        this.creditDecisionRepository = creditDecisionRepository;
        this.evaluationDomainRepository = evaluationDomainRepository;
        this.evaluationMapper = evaluationMapper;
        this.jpaEvaluationRepository = jpaEvaluationRepository;
        this.getEvaluationClassificationUseCase = getEvaluationClassificationUseCase;
        this.getClassificationByLevelUseCase = getClassificationByLevelUseCase;
        this.getEvaluationDetailUseCase = getEvaluationDetailUseCase;
        this.compareEvaluationsUseCase = compareEvaluationsUseCase;
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

    /** Retorna el resumen de clasificación de riesgo del portafolio. */
    @GetMapping("/clasificacion")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER','CREDIT_SUPERVISOR')")
    public ResponseEntity<ClassificationSummaryResponse> obtenerClasificacion(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaHasta) {
        var summary = getEvaluationClassificationUseCase.resumen(fechaDesde, fechaHasta);
        var response = new ClassificationSummaryResponse(
                summary.levels().stream()
                        .map(lc -> new LevelCountDto(lc.level().name(), lc.count()))
                        .toList(),
                summary.desde(),
                summary.hasta());
        return ResponseEntity.ok(response);
    }

    /** Lista la última evaluación por solicitante filtrada por nivel de riesgo. */
    @GetMapping("/clasificacion/{nivelRiesgo}")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER','CREDIT_SUPERVISOR')")
    public ResponseEntity<Page<ClassificationItemResponse>> listarPorNivel(
            @PathVariable RiskLevel nivelRiesgo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaHasta,
            @PageableDefault(size = 20) Pageable pageable) {
        PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize());
        PagedResult<ClassificationItem> result =
                getClassificationByLevelUseCase.porNivel(nivelRiesgo, fechaDesde, fechaHasta, pageRequest);
        List<ClassificationItemResponse> responseList = result.content().stream()
                .map(item -> new ClassificationItemResponse(
                        item.evaluationId(), item.applicantId(), item.applicantName(),
                        item.score(), item.level().name(), item.evaluatedAt(), item.evaluatedBy()))
                .toList();
        Page<ClassificationItemResponse> page = new PageImpl<>(
                responseList,
                pageable,
                result.totalElements());
        return ResponseEntity.ok(page);
    }

    /** Compara dos evaluaciones del mismo solicitante. */
    @GetMapping("/comparar")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<EvaluationComparisonResponse> compararEvaluaciones(
            @RequestParam UUID eval1,
            @RequestParam UUID eval2) {
        EvaluationComparison comparison = compareEvaluationsUseCase.comparar(eval1, eval2);
        EvaluationComparisonResponse response = new EvaluationComparisonResponse(
                toDetailResponse(comparison.eval1()),
                toDetailResponse(comparison.eval2()),
                comparison.scoreDelta());
        return ResponseEntity.ok(response);
    }

    /** Retorna el detalle completo de una evaluación por su identificador. */
    @GetMapping("/{id}/detalle")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<EvaluationDetailResponse> obtenerDetalle(@PathVariable UUID id) {
        EvaluationDetailView view = getEvaluationDetailUseCase.detalle(id);
        return ResponseEntity.ok(toDetailResponse(view));
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

    // Mapeo de EvaluationDetailView a EvaluationDetailResponse
    private EvaluationDetailResponse toDetailResponse(EvaluationDetailView view) {
        Evaluation e = view.evaluation();
        List<EvaluationDetailResponse.DetailItemDto> details = e.details().stream()
                .map(d -> new EvaluationDetailResponse.DetailItemDto(
                        d.id(), d.variableId(), d.variableName(),
                        d.rawValue(), d.score(), d.weight(), d.weightedScore()))
                .toList();
        List<EvaluationDetailResponse.KnockoutItemDto> knockouts = e.knockouts().stream()
                .map(k -> new EvaluationDetailResponse.KnockoutItemDto(
                        k.id(), k.ruleId(), k.ruleName(), k.fieldValue(), k.triggered()))
                .toList();
        return new EvaluationDetailResponse(
                e.id(), e.applicantId(), view.applicantName(),
                e.modelId(), view.modelName(), view.modelVersion(),
                e.financialDataId(), e.totalScore(), e.riskLevel().name(),
                e.knockedOut(), e.knockoutReasons(), e.evaluatedAt(), e.evaluatedBy(),
                details, knockouts);
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
