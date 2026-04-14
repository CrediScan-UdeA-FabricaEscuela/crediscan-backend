package co.udea.codefactory.creditscoring.scoringmodel.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.CreateKnockoutRuleRequest;
import co.udea.codefactory.creditscoring.scoringmodel.application.dto.UpdateKnockoutRuleRequest;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutOperator;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.ManageKnockoutRulesUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.KnockoutRuleRepositoryPort;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class KnockoutRuleService implements ManageKnockoutRulesUseCase {

    private final KnockoutRuleRepositoryPort koRepo;
    private final ScoringModelRepositoryPort modeloRepo;

    public KnockoutRuleService(
            KnockoutRuleRepositoryPort koRepo,
            ScoringModelRepositoryPort modeloRepo) {
        this.koRepo = koRepo;
        this.modeloRepo = modeloRepo;
    }

    @Override
    public KnockoutRule crear(UUID modeloId, CreateKnockoutRuleRequest request) {
        // Verificar que el modelo existe
        modeloRepo.findById(modeloId)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de scoring no encontrado: " + modeloId));

        KnockoutOperator operador = KnockoutOperator.valueOf(request.operador());
        KnockoutRule regla = KnockoutRule.crear(
                modeloId, request.campo(), operador,
                request.umbral(), request.mensaje(), request.prioridad());
        return koRepo.save(regla);
    }

    @Override
    public KnockoutRule actualizar(UUID id, UpdateKnockoutRuleRequest request) {
        KnockoutRule existente = koRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla knockout no encontrada: " + id));

        KnockoutOperator operador = KnockoutOperator.valueOf(request.operador());
        KnockoutRule actualizada = KnockoutRule.rehydrate(
                existente.id(), existente.modeloId(),
                request.campo(), operador,
                request.umbral(), request.mensaje(),
                request.prioridad(), request.activa());
        return koRepo.update(actualizada);
    }

    @Override
    public void eliminar(UUID id) {
        koRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla knockout no encontrada: " + id));
        koRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnockoutRule> listarPorModelo(UUID modeloId) {
        modeloRepo.findById(modeloId)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de scoring no encontrado: " + modeloId));
        return koRepo.findByModeloId(modeloId);
    }
}
