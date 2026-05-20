package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationComparison;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetailView;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.CompareEvaluationsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationDetailUseCase;

/**
 * Servicio de aplicación para comparar dos evaluaciones del mismo solicitante.
 * Reutiliza {@link GetEvaluationDetailUseCase} para obtener el detalle de cada evaluación.
 */
@Service
@Transactional(readOnly = true)
public class CompareEvaluationsService implements CompareEvaluationsUseCase {

    private final GetEvaluationDetailUseCase getEvaluationDetailUseCase;

    public CompareEvaluationsService(GetEvaluationDetailUseCase getEvaluationDetailUseCase) {
        this.getEvaluationDetailUseCase = getEvaluationDetailUseCase;
    }

    @Override
    public EvaluationComparison comparar(UUID eval1Id, UUID eval2Id) {
        EvaluationDetailView detail1 = getEvaluationDetailUseCase.detalle(eval1Id);
        EvaluationDetailView detail2 = getEvaluationDetailUseCase.detalle(eval2Id);

        if (!detail1.evaluation().applicantId().equals(detail2.evaluation().applicantId())) {
            throw new EvaluationValidationException(
                    "Las evaluaciones deben pertenecer al mismo solicitante");
        }

        BigDecimal scoreDelta = detail2.evaluation().totalScore()
                .subtract(detail1.evaluation().totalScore());

        return new EvaluationComparison(detail1, detail2, scoreDelta);
    }
}
