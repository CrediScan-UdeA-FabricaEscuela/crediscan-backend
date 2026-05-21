package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
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
import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ClassificationItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationComparison;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetail;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetailView;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationKnockout;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.DecisionFilterValue;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationStats;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.ExportFormat;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.CompareEvaluationsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.ExecuteEvaluationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.ExportEvaluationsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetClassificationByLevelUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationClassificationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationDetailUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationReportUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationStatsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.SearchEvaluationsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationListCsvPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.ClassificationItemResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.ClassificationSummaryResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.EvaluationComparisonResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.EvaluationDetailResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.EvaluationSearchItemDto;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.EvaluationSearchResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.EvaluationStatsResponse;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.LevelCountDto;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.EvaluationPersistenceMapper;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.JpaEvaluationRepository;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

/**
 * Controlador REST para el bounded context de evaluaciones crediticias.
 * Expone los endpoints para ejecutar evaluaciones, consultarlas, buscarlas y exportarlas.
 *
 * <p>ORDEN DE PATHS CRÍTICO: rutas estáticas literales ANTES de {@code /{id}} para evitar
 * colisiones con Spring MVC (e.g., /estadisticas no puede ser interpretado como UUID).</p>
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
    private final SearchEvaluationsUseCase searchEvaluationsUseCase;
    private final GetEvaluationStatsUseCase getEvaluationStatsUseCase;
    private final ExportEvaluationsUseCase exportEvaluationsUseCase;
    private final EvaluationListCsvPort csvPort;

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
            CompareEvaluationsUseCase compareEvaluationsUseCase,
            SearchEvaluationsUseCase searchEvaluationsUseCase,
            GetEvaluationStatsUseCase getEvaluationStatsUseCase,
            ExportEvaluationsUseCase exportEvaluationsUseCase,
            EvaluationListCsvPort csvPort) {
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
        this.searchEvaluationsUseCase = searchEvaluationsUseCase;
        this.getEvaluationStatsUseCase = getEvaluationStatsUseCase;
        this.exportEvaluationsUseCase = exportEvaluationsUseCase;
        this.csvPort = csvPort;
    }

    // =========================================================================
    // GET / — búsqueda avanzada paginada (reemplaza el scaffold sin filtros)
    // =========================================================================

    /**
     * Búsqueda avanzada paginada de evaluaciones con filtros opcionales.
     * Reemplaza el endpoint scaffold anterior (listado sin filtros).
     * {@code fecha_desde} y {@code fecha_hasta} son requeridos.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER','CREDIT_SUPERVISOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<EvaluationSearchResponse> buscarEvaluaciones(
            @RequestParam(name = "fecha_desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaDesde,
            @RequestParam(name = "fecha_hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaHasta,
            @RequestParam(name = "nivel", required = false) List<RiskLevel> niveles,
            @RequestParam(name = "puntaje_min", required = false) BigDecimal puntajeMin,
            @RequestParam(name = "puntaje_max", required = false) BigDecimal puntajeMax,
            @RequestParam(name = "decision", required = false) List<String> decisionRaw,
            @RequestParam(name = "analista", required = false) String analista,
            @PageableDefault(size = 25) Pageable pageable) {

        var criteria = new EvaluationSearchCriteria(
                fechaDesde, fechaHasta, niveles, puntajeMin, puntajeMax,
                parseDecisiones(decisionRaw), analista);
        var pageReq = new PageRequest(pageable.getPageNumber(), pageable.getPageSize());
        PagedResult<EvaluationSearchItem> result = searchEvaluationsUseCase.search(criteria, pageReq);
        return ResponseEntity.ok(toSearchResponse(result));
    }

    // =========================================================================
    // POST / — ejecutar nueva evaluación
    // =========================================================================

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

    // =========================================================================
    // GET /comparar
    // =========================================================================

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

    /**
     * Exporta el listado filtrado en CSV (streaming) o PDF (buffered, máx 1000 filas).
     * Path declarado antes de {@code /{id}} para evitar colisiones.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER','CREDIT_SUPERVISOR')")
    public ResponseEntity<?> exportar(
            @RequestParam(name = "formato") ExportFormat formato,
            @RequestParam(name = "fecha_desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaDesde,
            @RequestParam(name = "fecha_hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaHasta,
            @RequestParam(name = "nivel", required = false) List<RiskLevel> niveles,
            @RequestParam(name = "puntaje_min", required = false) BigDecimal puntajeMin,
            @RequestParam(name = "puntaje_max", required = false) BigDecimal puntajeMax,
            @RequestParam(name = "decision", required = false) List<String> decisionRaw,
            @RequestParam(name = "analista", required = false) String analista) {

        var criteria = new EvaluationSearchCriteria(
                fechaDesde, fechaHasta, niveles, puntajeMin, puntajeMax,
                parseDecisiones(decisionRaw), analista);

        if (formato == ExportFormat.CSV) {
            // CSV: se usa el repositorio directamente (sin la validación de tamaño del service)
            // porque el CSV no tiene límite estricto de filas.
            PagedResult<EvaluationSearchItem> page = evaluationDomainRepository
                    .search(criteria, new PageRequest(0, 10_000));
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            csvPort.escribir(baos, page.content().stream());
            byte[] csvBytes = baos.toByteArray();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=evaluaciones.csv")
                    .body(csvBytes);
        }

        // PDF: buffered con límite de 1000 filas
        ExportEvaluationsUseCase.ExportArtifact artifact =
                exportEvaluationsUseCase.export(criteria, ExportFormat.PDF);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + artifact.filename())
                .body(artifact.payload());
    }

    /**
     * Retorna estadísticas agregadas del conjunto filtrado de evaluaciones.
     * Path declarado antes de {@code /{id}} para evitar colisiones.
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER','CREDIT_SUPERVISOR')")
    public ResponseEntity<EvaluationStatsResponse> estadisticas(
            @RequestParam(name = "fecha_desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaDesde,
            @RequestParam(name = "fecha_hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fechaHasta,
            @RequestParam(name = "nivel", required = false) List<RiskLevel> niveles,
            @RequestParam(name = "puntaje_min", required = false) BigDecimal puntajeMin,
            @RequestParam(name = "puntaje_max", required = false) BigDecimal puntajeMax,
            @RequestParam(name = "decision", required = false) List<String> decisionRaw,
            @RequestParam(name = "analista", required = false) String analista) {

        var criteria = new EvaluationSearchCriteria(
                fechaDesde, fechaHasta, niveles, puntajeMin, puntajeMax,
                parseDecisiones(decisionRaw), analista);
        EvaluationStats stats = getEvaluationStatsUseCase.stats(criteria);
        return ResponseEntity.ok(toStatsResponse(stats));
    }

    // =========================================================================
    // GET /{id}/detalle y /{id}/pdf — DESPUÉS de rutas estáticas
    // =========================================================================

    /** Retorna el detalle completo de una evaluación por su identificador. */
    @GetMapping("/{id}/detalle")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<EvaluationDetailResponse> obtenerDetalle(@PathVariable UUID id) {
        EvaluationDetailView view = getEvaluationDetailUseCase.detalle(id);
        return ResponseEntity.ok(toDetailResponse(view));
    }

    /** Descarga el reporte PDF de una evaluación individual. */
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

    // =========================================================================
    // GET /{id} — ÚLTIMO (debe estar después de todas las rutas estáticas)
    // =========================================================================

    /** Consulta una evaluación por su identificador. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<EvaluationResponse> obtenerEvaluacion(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(getEvaluationUseCase.obtenerPorId(id)));
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Parsea el parámetro {@code decision} multi-value.
     * Soporta valores separados por coma o múltiples query params.
     * Normaliza a null si la lista resultante está vacía.
     */
    private List<DecisionFilterValue> parseDecisiones(List<String> raw) {
        if (raw == null || raw.isEmpty()) return null;
        List<DecisionFilterValue> parsed = raw.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try {
                        return DecisionFilterValue.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        throw new EvaluationValidationException("decision inválida: " + s);
                    }
                })
                .toList();
        return parsed.isEmpty() ? null : parsed;
    }

    private EvaluationSearchResponse toSearchResponse(PagedResult<EvaluationSearchItem> result) {
        List<EvaluationSearchItemDto> dtos = result.content().stream()
                .map(item -> new EvaluationSearchItemDto(
                        item.evaluationId(),
                        item.applicantId(),
                        item.applicantName(),
                        item.evaluatedAt(),
                        item.score(),
                        item.riskLevel().name(),
                        item.decisionStatus(),
                        item.analista()))
                .toList();
        return new EvaluationSearchResponse(
                dtos,
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages());
    }

    private EvaluationStatsResponse toStatsResponse(EvaluationStats stats) {
        List<LevelCountDto> distribution = stats.distribution().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new LevelCountDto(e.getKey().name(), e.getValue()))
                .toList();
        return new EvaluationStatsResponse(stats.total(), stats.averageScore(), distribution);
    }

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
