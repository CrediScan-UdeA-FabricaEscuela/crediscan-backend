package co.udea.codefactory.creditscoring.evaluation.application.service;

import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationStats;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationStatsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;

/**
 * Servicio de aplicación para estadísticas del conjunto filtrado de evaluaciones.
 */
@Service
public class GetEvaluationStatsService implements GetEvaluationStatsUseCase {

    private final EvaluationRepositoryPort repo;

    public GetEvaluationStatsService(EvaluationRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public EvaluationStats stats(EvaluationSearchCriteria criteria) {
        return repo.stats(criteria);
    }
}
