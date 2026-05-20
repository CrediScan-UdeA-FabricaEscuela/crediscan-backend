package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.evaluation.application.dto.EvaluationProperties;
import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ClassificationSummary;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationClassificationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;

/**
 * Servicio de aplicación para obtener el resumen de clasificación de riesgo del portafolio.
 */
@Service
@Transactional(readOnly = true)
public class GetEvaluationClassificationService implements GetEvaluationClassificationUseCase {

    private final EvaluationRepositoryPort evaluationRepositoryPort;
    private final EvaluationProperties evaluationProperties;

    public GetEvaluationClassificationService(EvaluationRepositoryPort evaluationRepositoryPort,
            EvaluationProperties evaluationProperties) {
        this.evaluationRepositoryPort = evaluationRepositoryPort;
        this.evaluationProperties = evaluationProperties;
    }

    @Override
    public ClassificationSummary resumen(OffsetDateTime desde, OffsetDateTime hasta) {
        OffsetDateTime hastaEfectivo = hasta != null ? hasta : OffsetDateTime.now();
        OffsetDateTime desdeEfectivo = desde != null
                ? desde
                : hastaEfectivo.minusDays(evaluationProperties.getDefaultRangeDays());

        if (desdeEfectivo.isAfter(hastaEfectivo)) {
            throw new EvaluationValidationException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        Map<RiskLevel, Long> counts =
                evaluationRepositoryPort.countLatestByRiskLevel(desdeEfectivo, hastaEfectivo);

        return ClassificationSummary.crear(counts, desdeEfectivo, hastaEfectivo);
    }
}
