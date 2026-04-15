package co.udea.codefactory.creditscoring.scoringengine.infrastructure.adapter.in.rest;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.udea.codefactory.creditscoring.scoringengine.application.dto.CalculateScoreResponse;
import co.udea.codefactory.creditscoring.scoringengine.application.dto.SaveScenarioRequest;
import co.udea.codefactory.creditscoring.scoringengine.application.dto.ScenarioResponse;
import co.udea.codefactory.creditscoring.scoringengine.application.dto.SimulateScoreRequest;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.KnockoutEvaluationDetail;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.SimulationScenario;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.VariableScoreDetail;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.in.SimulateScoreUseCase;

@RestController
@RequestMapping("/api/v1/scoring")
public class ScoringSimulationController {

    private final SimulateScoreUseCase useCase;

    public ScoringSimulationController(SimulateScoreUseCase useCase) {
        this.useCase = useCase;
    }

    /** CA1 — Simula el scoring sin persistir el resultado. */
    @PostMapping("/simular")
    public ResponseEntity<CalculateScoreResponse> simular(
            @Valid @RequestBody SimulateScoreRequest request) {
        ScoringResult resultado = useCase.simular(
                request.modeloId(), request.valoresVariables());
        return ResponseEntity.ok(toResponse(resultado));
    }

    /** CA6 — Guarda un escenario de simulación para reutilización. */
    @PostMapping("/simulaciones")
    public ResponseEntity<ScenarioResponse> guardarEscenario(
            @Valid @RequestBody SaveScenarioRequest request,
            Principal principal) {
        String usuario = principal != null ? principal.getName() : "system";
        SimulationScenario escenario = useCase.guardarEscenario(
                request.modeloId(), request.nombre(),
                request.descripcion(), request.valoresVariables(), usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(ScenarioResponse.from(escenario));
    }

    /** CA6 — Lista los escenarios guardados para un modelo. */
    @GetMapping("/simulaciones")
    public ResponseEntity<List<ScenarioResponse>> listarEscenarios(
            @RequestParam UUID modeloId) {
        List<ScenarioResponse> lista = useCase.listarEscenarios(modeloId)
                .stream().map(ScenarioResponse::from).toList();
        return ResponseEntity.ok(lista);
    }

    /** CA7 — Re-ejecuta un escenario guardado. */
    @PostMapping("/simulaciones/{id}/ejecutar")
    public ResponseEntity<CalculateScoreResponse> ejecutarEscenario(
            @PathVariable UUID id) {
        ScoringResult resultado = useCase.ejecutarEscenario(id);
        return ResponseEntity.ok(toResponse(resultado));
    }

    // -------------------------------------------------------------------------

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
