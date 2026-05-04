package co.udea.codefactory.creditscoring.creditdecision.application.service;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.creditdecision.application.dto.RegisterCreditDecisionCommand;
import co.udea.codefactory.creditscoring.creditdecision.domain.exception.CreditDecisionAlreadyExistsException;
import co.udea.codefactory.creditscoring.creditdecision.domain.exception.CreditDecisionKnockoutException;
import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;
import co.udea.codefactory.creditscoring.creditdecision.domain.model.DecisionStatus;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.in.RegisterCreditDecisionUseCase;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.CreditDecisionRepositoryPort;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.EscalationNotificationPort;
import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.GetEvaluationUseCase;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

/**
 * Servicio de aplicación que orquesta el registro de una decisión crediticia.
 *
 * <p>Flujo:
 * <ol>
 *   <li>Verificar que la evaluación existe (404 si no)</li>
 *   <li>Verificar que no exista ya una decisión para esa evaluación (409 si existe)</li>
 *   <li>Validar que la decisión sea un estado válido del enum</li>
 *   <li>Si la evaluación fue rechazada por knock-out, solo permitir REJECTED (400 si no)</li>
 *   <li>Crear la decisión con el usuario autenticado</li>
 *   <li>Persistir la decisión</li>
 * </ol>
 *
 * <p>Nota: La notificación de escalamiento (RN7) se deja como TODO — no hay
 * sistema de eventos implementado actualmente.</p>
 */
@Service
@Transactional
public class RegisterCreditDecisionService implements RegisterCreditDecisionUseCase {

    private final CreditDecisionRepositoryPort creditDecisionRepository;
    private final GetEvaluationUseCase getEvaluationUseCase;
    private final EscalationNotificationPort escalationNotificationPort;

    public RegisterCreditDecisionService(
            CreditDecisionRepositoryPort creditDecisionRepository,
            GetEvaluationUseCase getEvaluationUseCase,
            EscalationNotificationPort escalationNotificationPort) {
        this.creditDecisionRepository = creditDecisionRepository;
        this.getEvaluationUseCase = getEvaluationUseCase;
        this.escalationNotificationPort = escalationNotificationPort;
    }

    @Override
    public CreditDecision registrar(RegisterCreditDecisionCommand command) {
        // 1. Verificar que la evaluación existe
        Evaluation evaluation = getEvaluationUseCase.obtenerPorId(command.evaluationId());

        // 2. Verificar que no exista ya una decisión para esta evaluación (CA1)
        if (creditDecisionRepository.existsByEvaluationId(command.evaluationId())) {
            throw new CreditDecisionAlreadyExistsException(
                    "Ya existe una decisión para la evaluación " + command.evaluationId());
        }

        // 3. Validar que el estado de decisión sea válido
        DecisionStatus decisionStatus;
        try {
            decisionStatus = DecisionStatus.valueOf(command.decision());
        } catch (IllegalArgumentException e) {
            throw new co.udea.codefactory.creditscoring.creditdecision.domain.exception.CreditDecisionValidationException(
                    "Estado de decisión inválido: " + command.decision() + ". Debe ser APPROVED, REJECTED, MANUAL_REVIEW o ESCALATED");
        }

        // 4. Si la evaluación fue rechazada por knock-out, solo permitir REJECTED (RN1)
        if (evaluation.knockedOut() && decisionStatus != DecisionStatus.REJECTED) {
            throw new CreditDecisionKnockoutException(
                    "La evaluación " + command.evaluationId() + " fue rechazada por knock-out: solo se permite REJECTED");
        }

        // 5. Obtener el usuario autenticado del SecurityContext
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();

        // 6. Crear la decisión
        CreditDecision creditDecision = CreditDecision.crear(
                command.evaluationId(),
                decisionStatus,
                command.observations(),
                usuario
        );

        // 7. Persistir la decisión
        CreditDecision saved = creditDecisionRepository.save(creditDecision);

        // 8. Notificar al supervisor si la decisión es ESCALATED (CA7, RN4)
        if (decisionStatus == DecisionStatus.ESCALATED) {
            escalationNotificationPort.notifyEscalation(saved);
        }

        return saved;
    }
}
