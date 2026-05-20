package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.HistoryItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ModelInfo;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetApplicantEvaluationHistoryUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ScoringModelQueryPort;

/**
 * Servicio de aplicación para obtener el historial de evaluaciones de un solicitante.
 * Calcula el delta de puntaje entre evaluaciones consecutivas.
 */
@Service
@Transactional(readOnly = true)
public class GetApplicantEvaluationHistoryService implements GetApplicantEvaluationHistoryUseCase {

    private final EvaluationRepositoryPort evaluationRepositoryPort;
    private final ScoringModelQueryPort scoringModelQueryPort;

    public GetApplicantEvaluationHistoryService(EvaluationRepositoryPort evaluationRepositoryPort,
            ScoringModelQueryPort scoringModelQueryPort) {
        this.evaluationRepositoryPort = evaluationRepositoryPort;
        this.scoringModelQueryPort = scoringModelQueryPort;
    }

    @Override
    public List<HistoryItem> historial(UUID applicantId) {
        // La lista viene ordenada DESC desde el repositorio
        List<Evaluation> evaluations =
                evaluationRepositoryPort.findByApplicantIdOrderByEvaluatedAtDesc(applicantId);

        if (evaluations.isEmpty()) {
            return List.of();
        }

        // Batch lookup de modelos por IDs únicos
        Set<UUID> modelIds = evaluations.stream()
                .map(Evaluation::modelId)
                .collect(Collectors.toSet());

        Map<UUID, ModelInfo> modelInfoMap = new HashMap<>();
        for (UUID modelId : modelIds) {
            scoringModelQueryPort.findById(modelId)
                    .ifPresent(info -> modelInfoMap.put(modelId, info));
        }

        // Calcular scoreDelta iterando de más antigua (índice size-1) a más reciente (índice 0)
        // Guardamos los deltas indexados por posición en la lista original (DESC)
        int size = evaluations.size();
        BigDecimal[] deltas = new BigDecimal[size];

        // La evaluación en posición size-1 es la más antigua → delta = null
        deltas[size - 1] = null;

        // Iteramos desde la más antigua hacia la más reciente
        BigDecimal previousScore = evaluations.get(size - 1).totalScore();
        for (int i = size - 2; i >= 0; i--) {
            BigDecimal currentScore = evaluations.get(i).totalScore();
            deltas[i] = currentScore.subtract(previousScore);
            previousScore = currentScore;
        }

        // Construir lista de HistoryItems manteniendo el orden DESC
        List<HistoryItem> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Evaluation eval = evaluations.get(i);
            ModelInfo info = modelInfoMap.get(eval.modelId());
            String modelName = info != null ? info.name() : "";
            int modelVersion = info != null ? info.version() : 0;

            result.add(new HistoryItem(
                    eval.id(),
                    eval.evaluatedAt(),
                    eval.totalScore(),
                    eval.riskLevel(),
                    modelName,
                    modelVersion,
                    eval.evaluatedBy(),
                    eval.knockedOut(),
                    deltas[i]));
        }

        return result;
    }
}
