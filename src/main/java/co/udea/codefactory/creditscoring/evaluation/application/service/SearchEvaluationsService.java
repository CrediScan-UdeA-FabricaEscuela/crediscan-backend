package co.udea.codefactory.creditscoring.evaluation.application.service;

import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.SearchEvaluationsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

/**
 * Servicio de aplicación para la búsqueda avanzada paginada de evaluaciones.
 * Valida el tamaño de página máximo antes de delegar al repositorio.
 */
@Service
public class SearchEvaluationsService implements SearchEvaluationsUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final EvaluationRepositoryPort repo;

    public SearchEvaluationsService(EvaluationRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public PagedResult<EvaluationSearchItem> search(EvaluationSearchCriteria criteria, PageRequest page) {
        if (page.size() > MAX_PAGE_SIZE) {
            throw new EvaluationValidationException(
                    "size máximo permitido es " + MAX_PAGE_SIZE + ", recibido: " + page.size());
        }
        return repo.search(criteria, page);
    }
}
