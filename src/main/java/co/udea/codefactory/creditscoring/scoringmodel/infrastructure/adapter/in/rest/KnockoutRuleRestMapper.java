package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.in.rest;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.KnockoutRuleResponse;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;

@Component
public class KnockoutRuleRestMapper {

    public KnockoutRuleResponse toResponse(KnockoutRule rule) {
        return new KnockoutRuleResponse(
                rule.id(),
                rule.modeloId(),
                rule.campo(),
                rule.operador().name(),
                rule.umbral(),
                rule.mensaje(),
                rule.prioridad(),
                rule.activa());
    }
}
