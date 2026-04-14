package co.udea.codefactory.creditscoring.scoringengine.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.scoringengine.application.dto.CalculateScoreRequest;
import co.udea.codefactory.creditscoring.scoringengine.application.dto.CalculateScoreResponse;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.KnockoutEvaluationDetail;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.VariableScoreDetail;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.in.CalculateScoreUseCase;

@RestController
@RequestMapping("/api/v1/scoring")
public class ScoringEngineController {

    private final CalculateScoreUseCase useCase;

    public ScoringEngineController(CalculateScoreUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/calcular")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','RISK_MANAGER')")
    public ResponseEntity<CalculateScoreResponse> calcular(
            @Valid @RequestBody CalculateScoreRequest request) {
        ScoringResult resultado = useCase.calcular(request);
        return ResponseEntity.ok(toResponse(resultado));
    }

    private CalculateScoreResponse toResponse(ScoringResult r) {
        return new CalculateScoreResponse(
                r.modeloId(),
                r.aplicanteId(),
                r.puntajeFinal(),
                r.desglose().stream().map(this::toDetalleDto).toList(),
                r.reglasKoEvaluadas().stream().map(this::toKoDto).toList(),
                r.rechazadoPorKo(),
                r.mensajeKo());
    }

    private CalculateScoreResponse.VariableDetailDto toDetalleDto(VariableScoreDetail d) {
        return new CalculateScoreResponse.VariableDetailDto(
                d.variableId(), d.nombreVariable(), d.valorObservado(),
                d.etiquetaRango(), d.puntajeParcial(), d.peso(), d.contribucion());
    }

    private CalculateScoreResponse.KoEvaluacionDto toKoDto(KnockoutEvaluationDetail k) {
        return new CalculateScoreResponse.KoEvaluacionDto(
                k.reglaId(), k.campo(), k.operador().name(),
                k.umbral(), k.valorObservado(), k.activada(), k.mensaje());
    }
}
