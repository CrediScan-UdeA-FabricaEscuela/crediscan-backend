package co.udea.codefactory.creditscoring.evaluation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

/**
 * Tests unitarios de {@link SearchEvaluationsService}.
 */
class SearchEvaluationsServiceTest {

    private EvaluationRepositoryPort repo;
    private SearchEvaluationsService service;

    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2025-06-30T23:59:59Z");

    @BeforeEach
    void setUp() {
        repo = mock(EvaluationRepositoryPort.class);
        service = new SearchEvaluationsService(repo);
    }

    @Test
    void busquedaValida_delegaAlRepositorio() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, null, null, null, null, null);
        PageRequest page = new PageRequest(0, 25);
        PagedResult<EvaluationSearchItem> expected = new PagedResult<>(List.of(), 0, 0, 0, 25);
        when(repo.search(criteria, page)).thenReturn(expected);

        PagedResult<EvaluationSearchItem> result = service.search(criteria, page);

        assertThat(result).isSameAs(expected);
        verify(repo).search(criteria, page);
    }

    @Test
    void sizeMayorA100_lanzaEvaluationValidationException() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, null, null, null, null, null);
        PageRequest page = new PageRequest(0, 200);

        assertThatThrownBy(() -> service.search(criteria, page))
                .isInstanceOf(EvaluationValidationException.class);
    }
}
