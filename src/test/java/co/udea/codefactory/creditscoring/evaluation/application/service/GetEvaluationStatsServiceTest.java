package co.udea.codefactory.creditscoring.evaluation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationStats;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;

/**
 * Tests unitarios de {@link GetEvaluationStatsService}.
 */
class GetEvaluationStatsServiceTest {

    private EvaluationRepositoryPort repo;
    private GetEvaluationStatsService service;

    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2025-06-30T23:59:59Z");

    @BeforeEach
    void setUp() {
        repo = mock(EvaluationRepositoryPort.class);
        service = new GetEvaluationStatsService(repo);
    }

    @Test
    void stats_delegaAlRepositorioYRetornaResultado() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, null, null, null, null, null);
        EvaluationStats expected = new EvaluationStats(
                10L, new BigDecimal("75.50"),
                Map.of(RiskLevel.HIGH, 5L, RiskLevel.MEDIUM, 5L));
        when(repo.stats(criteria)).thenReturn(expected);

        EvaluationStats result = service.stats(criteria);

        assertThat(result).isSameAs(expected);
        verify(repo).stats(criteria);
    }
}
