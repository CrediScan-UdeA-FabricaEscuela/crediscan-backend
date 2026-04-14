package co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.in.rest;

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
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.scoring.application.dto.CreateScoringVariableRequest;
import co.udea.codefactory.creditscoring.scoring.application.dto.ScoringVariableListResponse;
import co.udea.codefactory.creditscoring.scoring.application.dto.ScoringVariableResponse;
import co.udea.codefactory.creditscoring.scoring.application.dto.UpdateScoringVariableRequest;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.port.in.CreateScoringVariableUseCase;
import co.udea.codefactory.creditscoring.scoring.domain.port.in.GetScoringVariablesUseCase;
import co.udea.codefactory.creditscoring.scoring.domain.port.in.UpdateScoringVariableUseCase;

@RestController
@RequestMapping("/api/v1/variables-scoring")
public class ScoringVariableController {

    private final CreateScoringVariableUseCase crearUseCase;
    private final UpdateScoringVariableUseCase actualizarUseCase;
    private final GetScoringVariablesUseCase listarUseCase;
    private final ScoringVariableRestMapper mapper;

    @Autowired
    public ScoringVariableController(
            CreateScoringVariableUseCase crearUseCase,
            UpdateScoringVariableUseCase actualizarUseCase,
            GetScoringVariablesUseCase listarUseCase,
            ScoringVariableRestMapper mapper) {
        this.crearUseCase = crearUseCase;
        this.actualizarUseCase = actualizarUseCase;
        this.listarUseCase = listarUseCase;
        this.mapper = mapper;
    }

    /** CA1: Crea una nueva variable de scoring. Solo ADMIN o RISK_MANAGER. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER')")
    public ResponseEntity<ScoringVariableResponse> crear(
            @Validated @RequestBody CreateScoringVariableRequest request) {
        ScoringVariable variable = crearUseCase.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(variable));
    }

    /** CA3: Actualiza una variable existente. Solo ADMIN o RISK_MANAGER. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER')")
    public ResponseEntity<ScoringVariableResponse> actualizar(
            @PathVariable UUID id,
            @Validated @RequestBody UpdateScoringVariableRequest request) {
        ScoringVariable variable = actualizarUseCase.actualizar(id, request);
        return ResponseEntity.ok(mapper.toResponse(variable));
    }

    /** CA7: Lista todas las variables con suma de pesos y advertencias del modelo. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RISK_MANAGER', 'ANALYST', 'CREDIT_SUPERVISOR')")
    public ResponseEntity<ScoringVariableListResponse> listar() {
        return ResponseEntity.ok(listarUseCase.listar());
    }
}
