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

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetailView;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ModelInfo;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ApplicantQueryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ScoringModelQueryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

/**
 * Tests unitarios para GetEvaluationDetailService.
 * Verifica 404 para id desconocido, resolución de nombre de modelo/applicant y fallback graceful.
 */
@ExtendWith(MockitoExtension.class)
class GetEvaluationDetailServiceTest {

    @Mock
    private EvaluationRepositoryPort evaluationRepositoryPort;

    @Mock
    private ScoringModelQueryPort scoringModelQueryPort;

    @Mock
    private ApplicantQueryPort applicantQueryPort;

    @InjectMocks
    private GetEvaluationDetailService service;

    private final UUID evaluationId = UUID.randomUUID();
    private final UUID applicantId = UUID.randomUUID();
    private final UUID modelId = UUID.randomUUID();

    @Test
    void detalle_idDesconocido_lanzaResourceNotFoundException() {
        when(evaluationRepositoryPort.findById(evaluationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalle(evaluationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void detalle_evaluacionExistente_resuelveModelNameYApplicantName() {
        Evaluation eval = buildEvaluation();
        when(evaluationRepositoryPort.findById(evaluationId)).thenReturn(Optional.of(eval));
        when(scoringModelQueryPort.findById(modelId))
                .thenReturn(Optional.of(new ModelInfo(modelId, "Modelo Test", 2)));
        when(applicantQueryPort.findNameById(applicantId)).thenReturn(Optional.of("Juan Pérez"));

        EvaluationDetailView result = service.detalle(evaluationId);

        assertThat(result.evaluation()).isEqualTo(eval);
        assertThat(result.modelName()).isEqualTo("Modelo Test");
        assertThat(result.modelVersion()).isEqualTo(2);
        assertThat(result.applicantName()).isEqualTo("Juan Pérez");
    }

    @Test
    void detalle_modeloNoEncontrado_fallbackAVacioYVersionCero() {
        Evaluation eval = buildEvaluation();
        when(evaluationRepositoryPort.findById(evaluationId)).thenReturn(Optional.of(eval));
        when(scoringModelQueryPort.findById(modelId)).thenReturn(Optional.empty());
        when(applicantQueryPort.findNameById(applicantId)).thenReturn(Optional.of("Juan"));

        EvaluationDetailView result = service.detalle(evaluationId);

        assertThat(result.modelName()).isEmpty();
        assertThat(result.modelVersion()).isZero();
        assertThat(result.applicantName()).isEqualTo("Juan");
    }

    @Test
    void detalle_applicantNoEncontrado_fallbackAStringVacio() {
        Evaluation eval = buildEvaluation();
        when(evaluationRepositoryPort.findById(evaluationId)).thenReturn(Optional.of(eval));
        when(scoringModelQueryPort.findById(modelId))
                .thenReturn(Optional.of(new ModelInfo(modelId, "Modelo", 1)));
        when(applicantQueryPort.findNameById(applicantId)).thenReturn(Optional.empty());

        EvaluationDetailView result = service.detalle(evaluationId);

        assertThat(result.applicantName()).isEmpty();
    }

    private Evaluation buildEvaluation() {
        return Evaluation.rehydrate(evaluationId, applicantId, modelId, UUID.randomUUID(),
                BigDecimal.valueOf(75), RiskLevel.LOW, false, null,
                OffsetDateTime.now(), "analista",
                OffsetDateTime.now(), "analista",
                List.of(), List.of());
    }
}
