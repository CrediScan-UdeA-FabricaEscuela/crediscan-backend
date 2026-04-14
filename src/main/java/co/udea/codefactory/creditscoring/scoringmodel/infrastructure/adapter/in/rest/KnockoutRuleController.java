package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.CreateKnockoutRuleRequest;
import co.udea.codefactory.creditscoring.scoringmodel.application.dto.KnockoutRuleResponse;
import co.udea.codefactory.creditscoring.scoringmodel.application.dto.UpdateKnockoutRuleRequest;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.ManageKnockoutRulesUseCase;

@RestController
@RequestMapping("/api/v1/modelos-scoring")
public class KnockoutRuleController {

    private final ManageKnockoutRulesUseCase useCase;
    private final KnockoutRuleRestMapper mapper;

    public KnockoutRuleController(ManageKnockoutRulesUseCase useCase, KnockoutRuleRestMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @GetMapping("/{modeloId}/reglas-knockout")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER','ANALYST')")
    public ResponseEntity<List<KnockoutRuleResponse>> listar(@PathVariable UUID modeloId) {
        List<KnockoutRuleResponse> respuesta = useCase.listarPorModelo(modeloId)
                .stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{modeloId}/reglas-knockout")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER')")
    public ResponseEntity<KnockoutRuleResponse> crear(
            @PathVariable UUID modeloId,
            @Valid @RequestBody CreateKnockoutRuleRequest request) {
        KnockoutRule regla = useCase.crear(modeloId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(regla));
    }

    @PutMapping("/{modeloId}/reglas-knockout/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER')")
    public ResponseEntity<KnockoutRuleResponse> actualizar(
            @PathVariable UUID modeloId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateKnockoutRuleRequest request) {
        KnockoutRule regla = useCase.actualizar(id, request);
        return ResponseEntity.ok(mapper.toResponse(regla));
    }

    @DeleteMapping("/{modeloId}/reglas-knockout/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RISK_MANAGER')")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID modeloId,
            @PathVariable UUID id) {
        useCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
