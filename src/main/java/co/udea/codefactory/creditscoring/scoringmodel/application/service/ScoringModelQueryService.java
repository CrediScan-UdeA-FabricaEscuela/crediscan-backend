package co.udea.codefactory.creditscoring.scoringmodel.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.ScoringModelComparisonResponse;
import co.udea.codefactory.creditscoring.scoringmodel.application.dto.ScoringModelComparisonResponse.DiferenciaVariable;
import co.udea.codefactory.creditscoring.scoringmodel.application.dto.ScoringModelResponse;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.CompareScoringModelsUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.GetScoringModelsUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class ScoringModelQueryService
        implements GetScoringModelsUseCase, CompareScoringModelsUseCase {

    private final ScoringModelRepositoryPort repositorio;

    @Autowired
    public ScoringModelQueryService(ScoringModelRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<ScoringModel> listar() {
        return repositorio.findAll();
    }

    @Override
    public ScoringModel obtener(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de scoring", "id", id));
    }

    @Override
    public ScoringModelComparisonResponse comparar(UUID idBase, UUID idComparado) {
        ScoringModel base = repositorio.findById(idBase)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de scoring", "id", idBase));
        ScoringModel comparado = repositorio.findById(idComparado)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de scoring", "id", idComparado));

        List<DiferenciaVariable> diferencias = calcularDiferencias(base, comparado);

        return new ScoringModelComparisonResponse(
                toResponse(base), toResponse(comparado), diferencias);
    }

    // -------------------------------------------------------------------------
    // Helpers de comparación
    // -------------------------------------------------------------------------

    private List<DiferenciaVariable> calcularDiferencias(ScoringModel base, ScoringModel comparado) {
        Map<UUID, ModelVariable> baseMap = new HashMap<>();
        for (ModelVariable mv : base.variables()) {
            baseMap.put(mv.variableId(), mv);
        }

        Map<UUID, ModelVariable> comparadoMap = new HashMap<>();
        for (ModelVariable mv : comparado.variables()) {
            comparadoMap.put(mv.variableId(), mv);
        }

        List<DiferenciaVariable> diferencias = new ArrayList<>();

        // Variables en base pero no en comparado → ELIMINADA
        for (UUID varId : baseMap.keySet()) {
            if (!comparadoMap.containsKey(varId)) {
                diferencias.add(new DiferenciaVariable(
                        varId, "ELIMINADA", baseMap.get(varId).peso(), null));
            }
        }

        // Variables en comparado pero no en base → AGREGADA
        for (UUID varId : comparadoMap.keySet()) {
            if (!baseMap.containsKey(varId)) {
                diferencias.add(new DiferenciaVariable(
                        varId, "AGREGADA", null, comparadoMap.get(varId).peso()));
            }
        }

        // Variables en ambos → MODIFICADA o SIN_CAMBIO
        for (UUID varId : baseMap.keySet()) {
            if (comparadoMap.containsKey(varId)) {
                ModelVariable mvBase = baseMap.get(varId);
                ModelVariable mvComparado = comparadoMap.get(varId);
                String tipo = mvBase.peso().compareTo(mvComparado.peso()) != 0
                        ? "MODIFICADA"
                        : "SIN_CAMBIO";
                diferencias.add(new DiferenciaVariable(
                        varId, tipo, mvBase.peso(), mvComparado.peso()));
            }
        }

        return diferencias;
    }

    // -------------------------------------------------------------------------
    // Mapeo dominio → DTO
    // -------------------------------------------------------------------------

    private ScoringModelResponse toResponse(ScoringModel modelo) {
        List<ScoringModelResponse.ModelVariableResponse> vars = modelo.variables().stream()
                .map(mv -> new ScoringModelResponse.ModelVariableResponse(
                        mv.id(), mv.variableId(), mv.peso(), mv.rangosSnapshot()))
                .toList();
        return new ScoringModelResponse(
                modelo.id(),
                modelo.nombre(),
                modelo.descripcion(),
                modelo.version(),
                modelo.estado().name(),
                vars,
                modelo.fechaCreacion(),
                modelo.fechaActivacion());
    }
}
