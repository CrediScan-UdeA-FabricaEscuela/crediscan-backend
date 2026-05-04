package co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.creditdecision.application.dto.RegisterCreditDecisionCommand;
import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.in.RegisterCreditDecisionUseCase;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.CreditDecisionRepositoryPort;
import co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.in.rest.dto.CreditDecisionResponse;
import co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.in.rest.dto.RegisterCreditDecisionRequest;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

/**
 * Controlador REST para registrar y consultar decisiones crediticias sobre evaluaciones.
 *
 * <p>El endpoint vive bajo /api/v1/evaluaciones/{id}/decision porque la decisión
 * es un sub-recurso de la evaluación (relación 1:1, FK a evaluation).</p>
 */
@RestController
@RequestMapping("/api/v1/evaluaciones")
public class CreditDecisionController {

    private final RegisterCreditDecisionUseCase registerCreditDecisionUseCase;
    private final CreditDecisionRepositoryPort creditDecisionRepository;

    public CreditDecisionController(RegisterCreditDecisionUseCase registerCreditDecisionUseCase,
            CreditDecisionRepositoryPort creditDecisionRepository) {
        this.registerCreditDecisionUseCase = registerCreditDecisionUseCase;
        this.creditDecisionRepository = creditDecisionRepository;
    }

    /**
     * Registra la decisión final sobre una evaluación crediticia.
     *
     * <p>CA1: Solo una decisión por evaluación (409 si ya existe).</p>
     * <p>CA3: Observaciones mínimo 20 caracteres (400 si no).</p>
     * <p>RN1: Si knock-out, solo REJECTED permitido (400 si no).</p>
     * <p>RN3: Solo RISK_MANAGER y ADMIN pueden registrar (403/401).</p>
     *
     * @param id identificador de la evaluación
     * @param request datos de la decisión
     * @return 201 con la decisión persistida
     */
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('RISK_MANAGER','ADMIN')")
    public ResponseEntity<CreditDecisionResponse> registrarDecision(
            @PathVariable UUID id,
            @Valid @RequestBody RegisterCreditDecisionRequest request) {
        CreditDecision decision = registerCreditDecisionUseCase.registrar(
                new RegisterCreditDecisionCommand(id, request.decision(), request.observations()));
        return ResponseEntity
                .created(URI.create("/api/v1/evaluaciones/" + id + "/decision"))
                .body(toResponse(decision));
    }

    /**
     * Consulta la decisión crediticia de una evaluación.
     *
     * <p>CA1: Permite al cliente verificar si ya existe una decisión.</p>
     *
     * @param id identificador de la evaluación
     * @return 200 con la decisión si existe, 404 si no
     */
    @GetMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<CreditDecisionResponse> obtenerDecision(@PathVariable UUID id) {
        return creditDecisionRepository.existsByEvaluationId(id)
                ? ResponseEntity.ok(toResponse(findDecision(id)))
                : ResponseEntity.notFound().build();
    }

    private CreditDecision findDecision(UUID evaluationId) {
        return creditDecisionRepository.findByEvaluationId(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("CreditDecision", "evaluationId", evaluationId));
    }

    private CreditDecisionResponse toResponse(CreditDecision d) {
        return new CreditDecisionResponse(
                d.id(), d.evaluationId(), d.decision().name(),
                d.observations(), d.analystId(), d.decidedAt(), d.createdAt(),
                d.supervisorId(), d.resolutionDeadlineAt());
    }
}
