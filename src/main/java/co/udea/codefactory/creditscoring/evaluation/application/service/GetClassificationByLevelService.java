package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.evaluation.application.dto.EvaluationProperties;
import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.ClassificationItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetClassificationByLevelUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.LatestEvaluationProjection;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

/**
 * Servicio de aplicación para listar la última evaluación por solicitante
 * filtrada por nivel de riesgo y rango de fechas.
 */
@Service
@Transactional(readOnly = true)
public class GetClassificationByLevelService implements GetClassificationByLevelUseCase {

    private final EvaluationRepositoryPort evaluationRepositoryPort;
    private final EvaluationProperties evaluationProperties;

    public GetClassificationByLevelService(EvaluationRepositoryPort evaluationRepositoryPort,
            EvaluationProperties evaluationProperties) {
        this.evaluationRepositoryPort = evaluationRepositoryPort;
        this.evaluationProperties = evaluationProperties;
    }

    @Override
    public PagedResult<ClassificationItem> porNivel(RiskLevel nivel, OffsetDateTime desde,
            OffsetDateTime hasta, PageRequest pageRequest) {
        OffsetDateTime hastaEfectivo = hasta != null ? hasta : OffsetDateTime.now();
        OffsetDateTime desdeEfectivo = desde != null
                ? desde
                : hastaEfectivo.minusDays(evaluationProperties.getDefaultRangeDays());

        if (desdeEfectivo.isAfter(hastaEfectivo)) {
            throw new EvaluationValidationException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        PagedResult<LatestEvaluationProjection> proyecciones =
                evaluationRepositoryPort.findLatestPerApplicantInRange(
                        nivel, desdeEfectivo, hastaEfectivo, pageRequest);

        List<ClassificationItem> items = proyecciones.content().stream()
                .map(this::toClassificationItem)
                .toList();

        return new PagedResult<>(
                items,
                proyecciones.totalElements(),
                proyecciones.totalPages(),
                proyecciones.pageNumber(),
                proyecciones.pageSize());
    }

    private ClassificationItem toClassificationItem(LatestEvaluationProjection p) {
        return new ClassificationItem(
                p.getEvaluationId(),
                p.getApplicantId(),
                p.getApplicantName(),
                p.getTotalScore(),
                RiskLevel.valueOf(p.getRiskLevel()),
                p.getEvaluatedAt(),
                p.getEvaluatedBy());
    }
}
