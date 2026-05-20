package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetailView;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ModelInfo;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationDetailUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ApplicantQueryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ScoringModelQueryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

/**
 * Servicio de aplicación para obtener el detalle completo de una evaluación.
 * Resuelve el nombre del modelo y del solicitante mediante puertos de salida cross-BC.
 */
@Service
@Transactional(readOnly = true)
public class GetEvaluationDetailService implements GetEvaluationDetailUseCase {

    private final EvaluationRepositoryPort evaluationRepositoryPort;
    private final ScoringModelQueryPort scoringModelQueryPort;
    private final ApplicantQueryPort applicantQueryPort;

    public GetEvaluationDetailService(EvaluationRepositoryPort evaluationRepositoryPort,
            ScoringModelQueryPort scoringModelQueryPort,
            ApplicantQueryPort applicantQueryPort) {
        this.evaluationRepositoryPort = evaluationRepositoryPort;
        this.scoringModelQueryPort = scoringModelQueryPort;
        this.applicantQueryPort = applicantQueryPort;
    }

    @Override
    public EvaluationDetailView detalle(UUID evaluationId) {
        Evaluation evaluation = evaluationRepositoryPort.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada"));

        ModelInfo modelInfo = scoringModelQueryPort.findById(evaluation.modelId())
                .orElse(null);

        String modelName = modelInfo != null ? modelInfo.name() : "";
        int modelVersion = modelInfo != null ? modelInfo.version() : 0;

        String applicantName = applicantQueryPort.findNameById(evaluation.applicantId())
                .orElse("");

        return new EvaluationDetailView(evaluation, modelName, modelVersion, applicantName);
    }
}
