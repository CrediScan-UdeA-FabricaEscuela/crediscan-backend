package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.evaluation.application.dto.EvaluationProperties;
import co.udea.codefactory.creditscoring.evaluation.application.dto.ExecuteEvaluationCommand;
import co.udea.codefactory.creditscoring.evaluation.domain.exception.ApplicantNoFinancialDataException;
import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationCooldownException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetail;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationKnockout;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.ExecuteEvaluationUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;
import co.udea.codefactory.creditscoring.scoringengine.application.dto.CalculateScoreRequest;
import co.udea.codefactory.creditscoring.scoringengine.domain.model.ScoringResult;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.in.CalculateScoreUseCase;

/**
 * Servicio de aplicación que orquesta la ejecución completa de una evaluación crediticia.
 *
 * <p>Flujo: verifica cooldown → calcula score → obtiene financialDataId
 * → clasifica riesgo → mapea detalles → persiste evaluación.</p>
 */
@Service
@Transactional
public class ExecuteEvaluationService implements ExecuteEvaluationUseCase {

    private final EvaluationRepositoryPort evaluationRepository;
    private final CalculateScoreUseCase calculateScoreUseCase;
    private final FinancialDataRepositoryPort financialDataRepository;
    private final EvaluationProperties evaluationProperties;

    public ExecuteEvaluationService(
            EvaluationRepositoryPort evaluationRepository,
            CalculateScoreUseCase calculateScoreUseCase,
            FinancialDataRepositoryPort financialDataRepository,
            EvaluationProperties evaluationProperties) {
        this.evaluationRepository = evaluationRepository;
        this.calculateScoreUseCase = calculateScoreUseCase;
        this.financialDataRepository = financialDataRepository;
        this.evaluationProperties = evaluationProperties;
    }

    @Override
    public Evaluation ejecutar(ExecuteEvaluationCommand command) {
        // 1. Verificar período de cooldown entre evaluaciones del mismo solicitante
        OffsetDateTime desde = OffsetDateTime.now()
                .minusHours(evaluationProperties.getCooldownHours());
        if (evaluationRepository.existsByApplicantIdAndEvaluatedAtAfter(
                command.applicantId(), desde)) {
            throw new EvaluationCooldownException(
                    "El solicitante " + command.applicantId()
                    + " ya fue evaluado en las últimas "
                    + evaluationProperties.getCooldownHours() + " horas");
        }

        // 2. Verificar que el solicitante tenga datos financieros antes de invocar el motor
        int maxVersion = financialDataRepository.findMaxVersionByApplicantId(command.applicantId())
                .orElseThrow(() -> new ApplicantNoFinancialDataException(
                        "El solicitante " + command.applicantId() + " no tiene datos financieros"));
        UUID financialDataId = financialDataRepository
                .findByApplicantIdAndVersion(command.applicantId(), maxVersion)
                .map(fd -> fd.id())
                .orElseThrow(() -> new ApplicantNoFinancialDataException(
                        "El solicitante " + command.applicantId() + " no tiene datos financieros"));

        // 3. Calcular el puntaje usando el motor de scoring
        ScoringResult resultado = calculateScoreUseCase.calcular(
                new CalculateScoreRequest(command.applicantId(), command.modelId()));

        // 4. Clasificar el nivel de riesgo (REJECTED si fue rechazado por knockout)
        RiskLevel riskLevel = resultado.rechazadoPorKo()
                ? RiskLevel.rejected()
                : RiskLevel.fromScore(resultado.puntajeFinal());

        // 5. Mapear el desglose de variables al modelo de dominio de evaluación
        List<EvaluationDetail> details = resultado.desglose().stream()
                .map(d -> EvaluationDetail.crear(
                        d.variableId(),
                        d.nombreVariable(),
                        d.valorObservado() != null ? d.valorObservado().toPlainString() : "0",
                        BigDecimal.valueOf(d.puntajeParcial()),
                        d.peso(),
                        d.contribucion()))
                .toList();

        // 6. Mapear los resultados de reglas knockout al modelo de dominio
        List<EvaluationKnockout> knockouts = resultado.reglasKoEvaluadas().stream()
                .map(ko -> EvaluationKnockout.crear(
                        ko.reglaId(),
                        ko.campo(),
                        ko.valorObservado() != null ? ko.valorObservado().toPlainString() : "0",
                        ko.activada()))
                .toList();

        // 7. Obtener el usuario autenticado del SecurityContext
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();

        // 8. Crear el agregado y persistirlo
        Evaluation evaluation = Evaluation.crear(
                command.applicantId(),
                resultado.modeloId(),
                financialDataId,
                resultado.puntajeFinal(),
                riskLevel,
                resultado.rechazadoPorKo(),
                resultado.mensajeKo(),
                usuario,
                details,
                knockouts);

        return evaluationRepository.save(evaluation);
    }
}
