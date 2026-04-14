package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.CreateScoringModelRequest;
import co.udea.codefactory.creditscoring.scoringmodel.application.dto.ScoringModelComparisonResponse;
import co.udea.codefactory.creditscoring.scoringmodel.application.dto.ScoringModelResponse;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.ActivateScoringModelUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.CompareScoringModelsUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.CreateScoringModelUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.GetScoringModelsUseCase;

@RestController
@RequestMapping("/api/v1/modelos-scoring")
public class ScoringModelController {

    private final CreateScoringModelUseCase crearUseCase;
    private final ActivateScoringModelUseCase activarUseCase;
    private final GetScoringModelsUseCase listarUseCase;
    private final CompareScoringModelsUseCase compararUseCase;
    private final ScoringModelRestMapper mapper;

    @Autowired
    public ScoringModelController(
            CreateScoringModelUseCase crearUseCase,
            ActivateScoringModelUseCase activarUseCase,
            GetScoringModelsUseCase listarUseCase,
            CompareScoringModelsUseCase compararUseCase,
            ScoringModelRestMapper mapper) {
        this.crearUseCase = crearUseCase;
        this.activarUseCase = activarUseCase;
        this.listarUseCase = listarUseCase;
        this.compararUseCase = compararUseCase;
        this.mapper = mapper;
    }

    /** CA1/CA2: Crea una nueva versión del modelo. Solo ADMIN o RISK_MANAGER. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER')")
    public ResponseEntity<ScoringModelResponse> crear(
            @Validated @RequestBody CreateScoringModelRequest request) {
        ScoringModel modelo = crearUseCase.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(modelo));
    }

    /** CA2: Lista todas las versiones del modelo. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER', 'ANALYST', 'CREDIT_SUPERVISOR')")
    public ResponseEntity<List<ScoringModelResponse>> listar() {
        List<ScoringModelResponse> respuesta = listarUseCase.listar().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    /** CA2: Obtiene una versión del modelo por su UUID. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER', 'ANALYST', 'CREDIT_SUPERVISOR')")
    public ResponseEntity<ScoringModelResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(listarUseCase.obtener(id)));
    }

    /** CA3/CA4: Activa la versión indicada; desactiva la anteriormente activa. Solo ADMIN o RISK_MANAGER. */
    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER')")
    public ResponseEntity<ScoringModelResponse> activar(@PathVariable UUID id) {
        ScoringModel activado = activarUseCase.activar(id);
        return ResponseEntity.ok(mapper.toResponse(activado));
    }

    /** CA6: Compara dos versiones del modelo. */
    @GetMapping("/comparar")
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER', 'ANALYST', 'CREDIT_SUPERVISOR')")
    public ResponseEntity<ScoringModelComparisonResponse> comparar(
            @RequestParam UUID base, @RequestParam UUID comparado) {
        return ResponseEntity.ok(compararUseCase.comparar(base, comparado));
    }
}
