package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

/**
 * Servicio de aplicación para consultar una evaluación por su identificador.
 */
@Service
@Transactional(readOnly = true)
public class GetEvaluationService implements GetEvaluationUseCase {

    private final EvaluationRepositoryPort evaluationRepository;

    public GetEvaluationService(EvaluationRepositoryPort evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    @Override
    public Evaluation obtenerPorId(UUID evaluationId) {
        return evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluacion", "id", evaluationId));
    }
}
