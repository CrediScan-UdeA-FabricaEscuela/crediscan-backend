package co.udea.codefactory.creditscoring.evaluation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.HistoryItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ModelInfo;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ScoringModelQueryPort;

/**
 * Tests unitarios para GetApplicantEvaluationHistoryService.
 * Verifica cálculo de scoreDelta y retorno de lista vacía para applicant sin evaluaciones.
 */
@ExtendWith(MockitoExtension.class)
class GetApplicantEvaluationHistoryServiceTest {

    @Mock
    private EvaluationRepositoryPort evaluationRepositoryPort;

    @Mock
    private ScoringModelQueryPort scoringModelQueryPort;

    @InjectMocks
    private GetApplicantEvaluationHistoryService service;

    private final UUID applicantId = UUID.randomUUID();
    private final UUID modelId = UUID.randomUUID();

    @Test
    void historial_sinEvaluaciones_retornaListaVacia() {
        when(evaluationRepositoryPort.findByApplicantIdOrderByEvaluatedAtDesc(applicantId))
                .thenReturn(List.of());

        List<HistoryItem> result = service.historial(applicantId);

        assertThat(result).isEmpty();
    }

    @Test
    void historial_unaEvaluacion_scoreDeltaEsNull() {
        Evaluation eval = buildEvaluation(UUID.randomUUID(), BigDecimal.valueOf(75),
                OffsetDateTime.now().minusDays(1));

        when(evaluationRepositoryPort.findByApplicantIdOrderByEvaluatedAtDesc(applicantId))
                .thenReturn(List.of(eval));
        when(scoringModelQueryPort.findById(modelId))
                .thenReturn(Optional.of(new ModelInfo(modelId, "Modelo Test", 1)));

        List<HistoryItem> result = service.historial(applicantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).scoreDelta()).isNull();
    }

    @Test
    void historial_tresEvaluaciones_scoreDeltaCorrectoYOldestNull() {
        // Orden DESC: eval3 > eval2 > eval1 (más reciente primero)
        OffsetDateTime t3 = OffsetDateTime.now();
        OffsetDateTime t2 = t3.minusDays(1);
        OffsetDateTime t1 = t3.minusDays(2);

        Evaluation eval3 = buildEvaluation(UUID.randomUUID(), BigDecimal.valueOf(80), t3);
        Evaluation eval2 = buildEvaluation(UUID.randomUUID(), BigDecimal.valueOf(70), t2);
        Evaluation eval1 = buildEvaluation(UUID.randomUUID(), BigDecimal.valueOf(60), t1);

        // Repositorio retorna en orden DESC
        when(evaluationRepositoryPort.findByApplicantIdOrderByEvaluatedAtDesc(applicantId))
                .thenReturn(List.of(eval3, eval2, eval1));
        when(scoringModelQueryPort.findById(modelId))
                .thenReturn(Optional.of(new ModelInfo(modelId, "Modelo Test", 1)));

        List<HistoryItem> result = service.historial(applicantId);

        assertThat(result).hasSize(3);

        // eval3 (más reciente): delta = 80 - 70 = 10
        assertThat(result.get(0).evaluationId()).isEqualTo(eval3.id());
        assertThat(result.get(0).scoreDelta()).isEqualByComparingTo(BigDecimal.valueOf(10));

        // eval2 (medio): delta = 70 - 60 = 10
        assertThat(result.get(1).evaluationId()).isEqualTo(eval2.id());
        assertThat(result.get(1).scoreDelta()).isEqualByComparingTo(BigDecimal.valueOf(10));

        // eval1 (más antigua): delta = null
        assertThat(result.get(2).evaluationId()).isEqualTo(eval1.id());
        assertThat(result.get(2).scoreDelta()).isNull();
    }

    private Evaluation buildEvaluation(UUID id, BigDecimal score, OffsetDateTime evaluatedAt) {
        return Evaluation.rehydrate(id, applicantId, modelId, UUID.randomUUID(),
                score, RiskLevel.LOW, false, null,
                evaluatedAt, "analista",
                evaluatedAt, "analista",
                List.of(), List.of());
    }
}
