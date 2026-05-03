package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.udea.codefactory.creditscoring.evaluation.application.dto.RegisterDecisionRequest;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.RegisterDecisionUseCase;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evaluaciones")
@RequiredArgsConstructor
public class EvaluationDecisionController {

    private final RegisterDecisionUseCase registerDecisionUseCase;


    @PostMapping("/{evaluationId}/decision")
    public ResponseEntity<Void> registerDecision(
            @PathVariable("evaluationId") UUID evaluationId,
            @RequestBody RegisterDecisionRequest request) {

        registerDecisionUseCase.register(evaluationId, request);
        return ResponseEntity.ok().build();
    }
}