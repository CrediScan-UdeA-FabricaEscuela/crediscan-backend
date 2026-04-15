package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationReportUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationReportPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

/**
 * Servicio de aplicación para generar el reporte PDF de una evaluación.
 */
@Service
@Transactional(readOnly = true)
public class GetEvaluationReportService implements GetEvaluationReportUseCase {

    private final EvaluationRepositoryPort evaluationRepository;
    private final EvaluationReportPort evaluationReportPort;

    public GetEvaluationReportService(EvaluationRepositoryPort evaluationRepository,
            EvaluationReportPort evaluationReportPort) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationReportPort = evaluationReportPort;
    }

    @Override
    public byte[] generarReporte(UUID evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluacion", "id", evaluationId));
        return evaluationReportPort.generar(evaluation);
    }
}
