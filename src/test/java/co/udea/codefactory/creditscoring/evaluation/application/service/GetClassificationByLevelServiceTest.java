package co.udea.codefactory.creditscoring.evaluation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.evaluation.application.dto.EvaluationProperties;
import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ClassificationItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.LatestEvaluationProjection;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

/**
 * Tests unitarios para GetClassificationByLevelService.
 * Verifica validación de fechas, paginación y mapeo de proyección a dominio.
 */
@ExtendWith(MockitoExtension.class)
class GetClassificationByLevelServiceTest {

    @Mock
    private EvaluationRepositoryPort evaluationRepositoryPort;

    @Mock
    private EvaluationProperties evaluationProperties;

    @InjectMocks
    private GetClassificationByLevelService service;

    @Test
    void porNivel_desdeAfterHasta_lanzaEvaluationValidationException() {
        OffsetDateTime desde = OffsetDateTime.now();
        OffsetDateTime hasta = desde.minusDays(1);
        PageRequest pageRequest = new PageRequest(0, 20);

        assertThatThrownBy(() -> service.porNivel(RiskLevel.HIGH, desde, hasta, pageRequest))
                .isInstanceOf(EvaluationValidationException.class);
    }

    @Test
    void porNivel_pageRequestPasaThroughAlRepositorio() {
        when(evaluationProperties.getDefaultRangeDays()).thenReturn(90);
        PageRequest pageRequest = new PageRequest(0, 5);
        PagedResult<LatestEvaluationProjection> emptyResult =
                new PagedResult<>(List.of(), 0L, 0, 0, 5);
        when(evaluationRepositoryPort.findLatestPerApplicantInRange(any(), any(), any(), eq(pageRequest)))
                .thenReturn(emptyResult);

        service.porNivel(RiskLevel.MEDIUM, null, null, pageRequest);

        verify(evaluationRepositoryPort).findLatestPerApplicantInRange(
                eq(RiskLevel.MEDIUM), any(), any(), eq(pageRequest));
    }

    @Test
    void porNivel_proyeccionMapeaAClassificationItem() {
        when(evaluationProperties.getDefaultRangeDays()).thenReturn(90);
        UUID evalId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();
        OffsetDateTime evaluatedAt = OffsetDateTime.now();

        LatestEvaluationProjection projection = buildProjection(
                evalId, applicantId, "Juan Pérez",
                BigDecimal.valueOf(45), "HIGH", evaluatedAt, "analista");

        PagedResult<LatestEvaluationProjection> resultado =
                new PagedResult<>(List.of(projection), 1L, 1, 0, 20);
        when(evaluationRepositoryPort.findLatestPerApplicantInRange(any(), any(), any(), any()))
                .thenReturn(resultado);

        PageRequest pageRequest = new PageRequest(0, 20);
        PagedResult<ClassificationItem> result = service.porNivel(RiskLevel.HIGH, null, null, pageRequest);

        assertThat(result.content()).hasSize(1);
        ClassificationItem item = result.content().get(0);
        assertThat(item.evaluationId()).isEqualTo(evalId);
        assertThat(item.applicantId()).isEqualTo(applicantId);
        assertThat(item.applicantName()).isEqualTo("Juan Pérez");
        assertThat(item.score()).isEqualByComparingTo(BigDecimal.valueOf(45));
        assertThat(item.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(item.evaluatedAt()).isEqualTo(evaluatedAt);
        assertThat(item.evaluatedBy()).isEqualTo("analista");
    }

    private LatestEvaluationProjection buildProjection(UUID evalId, UUID applicantId,
            String applicantName, BigDecimal totalScore, String riskLevel,
            OffsetDateTime evaluatedAt, String evaluatedBy) {
        return new LatestEvaluationProjection() {
            @Override public UUID getEvaluationId() { return evalId; }
            @Override public UUID getApplicantId() { return applicantId; }
            @Override public String getApplicantName() { return applicantName; }
            @Override public BigDecimal getTotalScore() { return totalScore; }
            @Override public String getRiskLevel() { return riskLevel; }
            @Override public OffsetDateTime getEvaluatedAt() { return evaluatedAt; }
            @Override public String getEvaluatedBy() { return evaluatedBy; }
        };
    }
}
