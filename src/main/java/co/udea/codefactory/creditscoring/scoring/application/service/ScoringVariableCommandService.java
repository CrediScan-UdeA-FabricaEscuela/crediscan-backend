package co.udea.codefactory.creditscoring.scoring.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.scoring.application.dto.CreateScoringVariableRequest;
import co.udea.codefactory.creditscoring.scoring.application.dto.UpdateScoringVariableRequest;
import co.udea.codefactory.creditscoring.scoring.application.dto.VariableCategoryRequest;
import co.udea.codefactory.creditscoring.scoring.application.dto.VariableRangeRequest;
import co.udea.codefactory.creditscoring.scoring.domain.exception.ScoringVariableValidationException;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableCategory;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableType;
import co.udea.codefactory.creditscoring.scoring.domain.port.in.CreateScoringVariableUseCase;
import co.udea.codefactory.creditscoring.scoring.domain.port.in.UpdateScoringVariableUseCase;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class ScoringVariableCommandService
        implements CreateScoringVariableUseCase, UpdateScoringVariableUseCase {

    private final ScoringVariableRepositoryPort repositorio;

    @Autowired
    public ScoringVariableCommandService(ScoringVariableRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public ScoringVariable crear(CreateScoringVariableRequest request) {
        if (repositorio.existsByNombre(request.nombre())) {
            throw new ScoringVariableValidationException(
                    "Ya existe una variable de scoring con el nombre: " + request.nombre());
        }

        VariableType tipo = VariableType.fromValor(request.tipo());
        ScoringVariable variable = ScoringVariable.crear(
                request.nombre(),
                request.descripcion(),
                tipo,
                request.peso(),
                mapearRangos(request.rangos(), null),
                mapearCategorias(request.categorias(), null));

        return repositorio.save(variable);
    }

    @Override
    public ScoringVariable actualizar(UUID id, UpdateScoringVariableRequest request) {
        ScoringVariable existente = repositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variable de scoring", "id", id));

        // Si el nombre cambió, verificar que no exista en otra variable (CA2)
        if (!existente.nombre().equalsIgnoreCase(request.nombre())
                && repositorio.existsByNombre(request.nombre())) {
            throw new ScoringVariableValidationException(
                    "Ya existe una variable de scoring con el nombre: " + request.nombre());
        }

        List<VariableRange> rangos = mapearRangos(request.rangos(), id);
        List<VariableCategory> categorias = mapearCategorias(request.categorias(), id);

        ScoringVariable actualizada = ScoringVariable.rehydrate(
                id,
                request.nombre(),
                request.descripcion(),
                existente.tipo(),
                request.peso(),
                request.activa(),
                rangos,
                categorias);

        return repositorio.update(actualizada);
    }

    // -------------------------------------------------------------------------
    // Helpers de mapeo
    // -------------------------------------------------------------------------

    private List<VariableRange> mapearRangos(List<VariableRangeRequest> rangoRequests, UUID variableId) {
        if (rangoRequests == null || rangoRequests.isEmpty()) {
            return List.of();
        }
        return rangoRequests.stream()
                .map(r -> new VariableRange(
                        UUID.randomUUID(),
                        variableId,
                        r.limiteInferior(),
                        r.limiteSuperior(),
                        r.puntaje(),
                        r.etiqueta()))
                .toList();
    }

    private List<VariableCategory> mapearCategorias(
            List<VariableCategoryRequest> catRequests, UUID variableId) {
        if (catRequests == null || catRequests.isEmpty()) {
            return List.of();
        }
        return catRequests.stream()
                .map(c -> new VariableCategory(
                        UUID.randomUUID(),
                        variableId,
                        c.categoria(),
                        c.puntaje(),
                        c.etiqueta()))
                .toList();
    }
}
