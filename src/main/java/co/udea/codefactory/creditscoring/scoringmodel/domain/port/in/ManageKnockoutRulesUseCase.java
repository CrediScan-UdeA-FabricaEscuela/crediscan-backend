package co.udea.codefactory.creditscoring.scoringmodel.domain.port.in;

import java.util.List;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.CreateKnockoutRuleRequest;
import co.udea.codefactory.creditscoring.scoringmodel.application.dto.UpdateKnockoutRuleRequest;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;

public interface ManageKnockoutRulesUseCase {

    KnockoutRule crear(UUID modeloId, CreateKnockoutRuleRequest request);

    KnockoutRule actualizar(UUID id, UpdateKnockoutRuleRequest request);

    void eliminar(UUID id);

    List<KnockoutRule> listarPorModelo(UUID modeloId);
}
