package co.udea.codefactory.creditscoring.creditdecision.domain.port.out;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;

/**
 * Puerto de salida para notificar al supervisor cuando una decisión es ESCALATED (CA7).
 *
 * <p>RN4: La notificación debe incluir el deadline de resolución (48h).</p>
 */
public interface EscalationNotificationPort {

    /**
     * Envía una notificación al supervisor sobre una decisión escalada.
     *
     * @param decision la decisión ESCALATED que requiere atención del supervisor
     */
    void notifyEscalation(CreditDecision decision);
}
