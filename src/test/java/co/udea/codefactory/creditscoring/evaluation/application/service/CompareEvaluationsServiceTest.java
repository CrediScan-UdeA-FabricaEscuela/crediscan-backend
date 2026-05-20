package co.udea.codefactory.creditscoring.evaluation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationComparison;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ModelInfo;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ApplicantQueryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ScoringModelQueryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

/**
 * Tests unitarios para CompareEvaluationsService.
 * Verifica validación de mismo applicant, cálculo de delta y propagación de 404.
 */
@ExtendWith(MockitoExtension.class)
class CompareEvaluationsServiceTest {

    @Mock
    private EvaluationRepositoryPort evaluationRepositoryPort;

    @Mock
    private ScoringModelQueryPort scoringModelQueryPort;

    @Mock
    private ApplicantQueryPort applicantQueryPort;

    @InjectMocks
    private GetEvaluationDetailService detailService;

    // CompareEvaluationsService necesita el use case de detalle
    private CompareEvaluationsService buildService() {
        return new CompareEvaluationsService(detailService);
    }

    private final UUID applicantId = UUID.randomUUID();
    private final UUID modelId = UUID.randomUUID();

    @Test
    void comparar_distinctApplicants_lanzaEvaluationValidationException() {
        UUID eval1Id = UUID.randomUUID();
        UUID eval2Id = UUID.randomUUID();
        UUID otroApplicant = UUID.randomUUID();

        Evaluation eval1 = buildEvaluation(eval1Id, applicantId, BigDecimal.valueOf(70));
        Evaluation eval2 = buildEvaluation(eval2Id, otroApplicant, BigDecimal.valueOf(80));

        when(evaluationRepositoryPort.findById(eval1Id)).thenReturn(Optional.of(eval1));
        when(evaluationRepositoryPort.findById(eval2Id)).thenReturn(Optional.of(eval2));
        when(scoringModelQueryPort.findById(modelId))
                .thenReturn(Optional.of(new ModelInfo(modelId, "Modelo", 1)));
        when(applicantQueryPort.findNameById(applicantId)).thenReturn(Optional.of("Juan"));
        when(applicantQueryPort.findNameById(otroApplicant)).thenReturn(Optional.of("Maria"));

        assertThatThrownBy(() -> buildService().comparar(eval1Id, eval2Id))
                .isInstanceOf(EvaluationValidationException.class);
    }

    @Test
    void comparar_mismoApplicant_retornaComparisonConDeltaCorrecto() {
        UUID eval1Id = UUID.randomUUID();
        UUID eval2Id = UUID.randomUUID();

        Evaluation eval1 = buildEvaluation(eval1Id, applicantId, BigDecimal.valueOf(70));
        Evaluation eval2 = buildEvaluation(eval2Id, applicantId, BigDecimal.valueOf(80));

        when(evaluationRepositoryPort.findById(eval1Id)).thenReturn(Optional.of(eval1));
        when(evaluationRepositoryPort.findById(eval2Id)).thenReturn(Optional.of(eval2));
        when(scoringModelQueryPort.findById(modelId))
                .thenReturn(Optional.of(new ModelInfo(modelId, "Modelo", 1)));
        when(applicantQueryPort.findNameById(applicantId)).thenReturn(Optional.of("Juan"));

        EvaluationComparison result = buildService().comparar(eval1Id, eval2Id);

        // scoreDelta = eval2.totalScore - eval1.totalScore = 80 - 70 = 10
        assertThat(result.scoreDelta()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(result.eval1().evaluation().id()).isEqualTo(eval1Id);
        assertThat(result.eval2().evaluation().id()).isEqualTo(eval2Id);
    }

    @Test
    void comparar_eval1IgualEval2_deltaEsCero() {
        UUID evalId = UUID.randomUUID();
        Evaluation eval = buildEvaluation(evalId, applicantId, BigDecimal.valueOf(75));

        when(evaluationRepositoryPort.findById(evalId)).thenReturn(Optional.of(eval));
        when(scoringModelQueryPort.findById(modelId))
                .thenReturn(Optional.of(new ModelInfo(modelId, "Modelo", 1)));
        when(applicantQueryPort.findNameById(applicantId)).thenReturn(Optional.of("Juan"));

        EvaluationComparison result = buildService().comparar(evalId, evalId);

        assertThat(result.scoreDelta()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void comparar_idDesconocido_propagaResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        UUID eval2Id = UUID.randomUUID();

        when(evaluationRepositoryPort.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buildService().comparar(unknownId, eval2Id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Evaluation buildEvaluation(UUID id, UUID applicant, BigDecimal score) {
        return Evaluation.rehydrate(id, applicant, modelId, UUID.randomUUID(),
                score, RiskLevel.LOW, false, null,
                OffsetDateTime.now(), "analista",
                OffsetDateTime.now(), "analista",
                List.of(), List.of());
    }
}
