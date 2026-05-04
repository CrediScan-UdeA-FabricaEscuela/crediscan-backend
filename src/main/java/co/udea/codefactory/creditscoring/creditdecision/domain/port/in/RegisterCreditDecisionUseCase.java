package co.udea.codefactory.creditscoring.creditdecision.domain.port.in;

import co.udea.codefactory.creditscoring.creditdecision.application.dto.RegisterCreditDecisionCommand;
import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;

/**
 * Caso de uso principal: registrar una decisión crediticia sobre una evaluación.
 */
public interface RegisterCreditDecisionUseCase {

    /**
     * Registra la decisión final sobre una evaluación crediticia.
     *
     * @param command datos de la decisión a registrar
     * @return la decisión persistida con su resultado
     * @throws co.udea.codefactory.creditscoring.creditdecision.domain.exception.CreditDecisionAlreadyExistsException
     *         si ya existe una decisión para esta evaluación (CA1)
     * @throws co.udea.codefactory.creditscoring.creditdecision.domain.exception.CreditDecisionKnockoutException
     *         si la evaluación fue rechazada por knock-out y no se registra REJECTED (RN1)
     */
    CreditDecision registrar(RegisterCreditDecisionCommand command);
}
