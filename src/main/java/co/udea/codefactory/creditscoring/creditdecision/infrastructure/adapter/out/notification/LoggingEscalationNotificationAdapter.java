package co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.out.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.EscalationNotificationPort;

/**
 * Adaptador de notificación de escalamiento.
 *
 * <p>Implementación inicial vía registro estructurado. Cuando se integre un sistema
 * de eventos (Kafka, RabbitMQ, email, etc.) este adaptador se reemplaza sin tocar
 * el dominio ni el servicio de aplicación.</p>
 */
@Component
public class LoggingEscalationNotificationAdapter implements EscalationNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEscalationNotificationAdapter.class);

    @Override
    public void notifyEscalation(CreditDecision decision) {
        log.warn(
            "ESCALATION_REQUIRED evaluationId={} decisionId={} analystId={} deadline={}",
            decision.evaluationId(),
            decision.id(),
            decision.analystId(),
            decision.resolutionDeadlineAt()
        );
    }
}
