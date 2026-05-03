package co.udea.codefactory.creditscoring.evaluation.application.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.evaluation.application.dto.RegisterDecisionRequest;
import co.udea.codefactory.creditscoring.evaluation.domain.model.CreditDecision;
import co.udea.codefactory.creditscoring.evaluation.domain.model.DecisionType;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.RegisterDecisionUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.CreditDecisionRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.NotificationPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Transactional
public class RegisterDecisionService implements RegisterDecisionUseCase {

    private final EvaluationRepositoryPort evaluationRepository;
    private final CreditDecisionRepositoryPort decisionRepository;
    private final NotificationPort notificationPort;

    @Override
    public void register(UUID evaluationId, RegisterDecisionRequest request) {

        // 1. Buscar evaluación
        var evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada"));

        // 2. CA1: validar que no exista decisión previa
        if (decisionRepository.existsByEvaluationId(evaluationId)) {
            throw new RuntimeException("Ya existe una decisión para esta evaluación");
        }

        // 3. RN3: validar analista asignado
        if (!evaluation.isAssignedTo(request.getAnalyst())) {
            throw new RuntimeException("Analista no autorizado");
        }

        DecisionType decisionType = request.getDecision();

        // 4. CA4 + RN1: validar knockout
        if (evaluation.isKnockout() && decisionType != DecisionType.REJECTED) {
            throw new RuntimeException("Solo se permite REJECTED si hay knockout");
        }

        // 5. Crear decisión
        CreditDecision decision = new CreditDecision(
                null,
                evaluationId,
                decisionType,
                request.getObservations(),
                null, // approvedAmount
                null, // interestRate
                null, // termMonths
                OffsetDateTime.now(),
                request.getAnalyst(),
                OffsetDateTime.now(),
                request.getAnalyst()
        );

        // 6. Guardar
        decisionRepository.save(decision);

        // 7. CA7: notificar SOLO si es ESCALATED
        if (decisionType == DecisionType.ESCALATED) {
            notificationPort.notifySupervisor(evaluationId);
        }
    }
}