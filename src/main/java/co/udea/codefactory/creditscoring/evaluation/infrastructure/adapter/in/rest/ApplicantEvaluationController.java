package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetApplicantEvaluationHistoryUseCase;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto.EvaluationHistoryItemResponse;

/**
 * Controlador REST para el historial de evaluaciones de un solicitante.
 * Aunque la ruta usa el prefijo {@code /api/v1/solicitantes}, la lógica
 * pertenece al BC evaluation (ADR-7).
 */
@RestController
@RequestMapping("/api/v1/solicitantes")
public class ApplicantEvaluationController {

    private final GetApplicantEvaluationHistoryUseCase getApplicantEvaluationHistoryUseCase;

    public ApplicantEvaluationController(
            GetApplicantEvaluationHistoryUseCase getApplicantEvaluationHistoryUseCase) {
        this.getApplicantEvaluationHistoryUseCase = getApplicantEvaluationHistoryUseCase;
    }

    /**
     * Retorna el historial de evaluaciones de un solicitante, ordenado por fecha descendente.
     * Si el solicitante no tiene evaluaciones retorna 200 con lista vacía.
     *
     * @param id identificador del solicitante
     * @return lista de ítems de historial con scoreDelta calculado
     */
    @GetMapping("/{id}/evaluaciones")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','CREDIT_SUPERVISOR','RISK_MANAGER')")
    public ResponseEntity<List<EvaluationHistoryItemResponse>> obtenerHistorial(
            @PathVariable UUID id) {
        List<EvaluationHistoryItemResponse> response =
                getApplicantEvaluationHistoryUseCase.historial(id).stream()
                        .map(item -> new EvaluationHistoryItemResponse(
                                item.evaluationId(),
                                item.evaluatedAt(),
                                item.totalScore(),
                                item.riskLevel().name(),
                                item.modelName(),
                                item.modelVersion(),
                                item.evaluatedBy(),
                                item.knockedOut(),
                                item.scoreDelta()))
                        .toList();
        return ResponseEntity.ok(response);
    }
}
