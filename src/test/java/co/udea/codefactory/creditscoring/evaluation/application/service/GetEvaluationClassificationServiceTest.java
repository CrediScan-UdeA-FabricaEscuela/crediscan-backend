package co.udea.codefactory.creditscoring.evaluation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.evaluation.application.dto.EvaluationProperties;
import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ClassificationSummary;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;

/**
 * Tests unitarios para GetEvaluationClassificationService.
 * Verifica lógica de rango de fechas, validación y construcción del resumen.
 */
@ExtendWith(MockitoExtension.class)
class GetEvaluationClassificationServiceTest {

    @Mock
    private EvaluationRepositoryPort evaluationRepositoryPort;

    @Mock
    private EvaluationProperties evaluationProperties;

    @InjectMocks
    private GetEvaluationClassificationService service;

    @Test
    void resumen_ambasFechasNull_usaRangoDefault() {
        when(evaluationProperties.getDefaultRangeDays()).thenReturn(90);
        when(evaluationRepositoryPort.countLatestByRiskLevel(any(), any()))
                .thenReturn(Map.of());

        OffsetDateTime antes = OffsetDateTime.now().minusDays(91);

        service.resumen(null, null);

        ArgumentCaptor<OffsetDateTime> desdeCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> hastaCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(evaluationRepositoryPort).countLatestByRiskLevel(desdeCaptor.capture(), hastaCaptor.capture());

        assertThat(desdeCaptor.getValue()).isAfter(antes);
        assertThat(hastaCaptor.getValue()).isAfterOrEqualTo(desdeCaptor.getValue());
    }

    @Test
    void resumen_fechaDesdeNull_calculaDesdeBasadaEnDefault() {
        when(evaluationProperties.getDefaultRangeDays()).thenReturn(90);
        when(evaluationRepositoryPort.countLatestByRiskLevel(any(), any()))
                .thenReturn(Map.of());

        OffsetDateTime hasta = OffsetDateTime.now();
        service.resumen(null, hasta);

        ArgumentCaptor<OffsetDateTime> desdeCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(evaluationRepositoryPort).countLatestByRiskLevel(desdeCaptor.capture(), any());

        assertThat(desdeCaptor.getValue()).isBefore(hasta);
    }

    @Test
    void resumen_desdeAfterHasta_lanzaEvaluationValidationException() {
        OffsetDateTime desde = OffsetDateTime.now();
        OffsetDateTime hasta = desde.minusDays(1);

        assertThatThrownBy(() -> service.resumen(desde, hasta))
                .isInstanceOf(EvaluationValidationException.class);
    }

    @Test
    void resumen_mapVacio_retornaSeisnivelesConCeros() {
        when(evaluationProperties.getDefaultRangeDays()).thenReturn(90);
        when(evaluationRepositoryPort.countLatestByRiskLevel(any(), any()))
                .thenReturn(Map.of());

        ClassificationSummary result = service.resumen(null, null);

        assertThat(result.levels()).hasSize(6);
        assertThat(result.levels()).allMatch(lc -> lc.count() == 0);
    }

    @Test
    void resumen_mapConDatos_retornaSeisnivelesConCountsCorrectos() {
        when(evaluationProperties.getDefaultRangeDays()).thenReturn(90);
        when(evaluationRepositoryPort.countLatestByRiskLevel(any(), any()))
                .thenReturn(Map.of(RiskLevel.HIGH, 5L, RiskLevel.MEDIUM, 3L));

        ClassificationSummary result = service.resumen(null, null);

        assertThat(result.levels()).hasSize(6);
        long highCount = result.levels().stream()
                .filter(lc -> lc.level() == RiskLevel.HIGH)
                .mapToLong(lc -> lc.count())
                .sum();
        assertThat(highCount).isEqualTo(5L);
    }
}
