package co.udea.codefactory.creditscoring.scoringmodel.application.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.scoringmodel.application.dto.CreateScoringModelRequest;
import co.udea.codefactory.creditscoring.scoringmodel.domain.exception.ScoringModelValidationException;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.ActivateScoringModelUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.in.CreateScoringModelUseCase;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;
import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class ScoringModelCommandService
        implements CreateScoringModelUseCase, ActivateScoringModelUseCase {

    private final ScoringModelRepositoryPort modeloRepo;
    private final ScoringVariableRepositoryPort variableRepo;
    private final ObjectMapper objectMapper;

    @Autowired
    public ScoringModelCommandService(
            ScoringModelRepositoryPort modeloRepo,
            ScoringVariableRepositoryPort variableRepo,
            ObjectMapper objectMapper) {
        this.modeloRepo = modeloRepo;
        this.variableRepo = variableRepo;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // crear()
    // -------------------------------------------------------------------------

    @Override
    public ScoringModel crear(CreateScoringModelRequest request) {
        if (modeloRepo.existsByNombre(request.nombre())) {
            throw new ScoringModelValidationException(
                    "Ya existe una versión del modelo con el nombre: " + request.nombre());
        }

        List<ModelVariable> variables;
        if (request.clonarDesde() != null) {
            variables = clonarVariables(request.clonarDesde());
        } else {
            variables = variablesDesdeActivas();
        }

        int siguienteVersion = modeloRepo.maxVersion() + 1;
        ScoringModel modelo = ScoringModel.crear(
                request.nombre(), request.descripcion(), siguienteVersion, variables);
        return modeloRepo.save(modelo);
    }

    private List<ModelVariable> clonarVariables(UUID modeloOrigenId) {
        ScoringModel origen = modeloRepo.findById(modeloOrigenId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Modelo de scoring", "id", modeloOrigenId));
        return origen.variables().stream()
                .map(v -> new ModelVariable(UUID.randomUUID(), null, v.variableId(), v.peso(), null))
                .toList();
    }

    private List<ModelVariable> variablesDesdeActivas() {
        return variableRepo.findAllActivas().stream()
                .map(v -> new ModelVariable(UUID.randomUUID(), null, v.id(), v.peso(), null))
                .toList();
    }

    // -------------------------------------------------------------------------
    // activar()
    // -------------------------------------------------------------------------

    @Override
    public ScoringModel activar(UUID id) {
        ScoringModel modelo = modeloRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de scoring", "id", id));

        // Generar snapshot de los rangos actuales para cada variable del modelo
        List<ModelVariable> variablesConSnapshot = modelo.variables().stream()
                .map(mv -> {
                    ScoringVariable variable = variableRepo.findById(mv.variableId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Variable de scoring", "id", mv.variableId()));
                    return new ModelVariable(
                            mv.id(), mv.modeloId(), mv.variableId(),
                            mv.peso(), buildSnapshot(variable));
                })
                .toList();

        ScoringModel activado = modelo.activar(OffsetDateTime.now(), variablesConSnapshot);

        // Desactivar el modelo previamente activo (CA4)
        modeloRepo.findActive().ifPresent(previo -> modeloRepo.update(previo.desactivar()));

        return modeloRepo.update(activado);
    }

    // -------------------------------------------------------------------------
    // Snapshot
    // -------------------------------------------------------------------------

    private String buildSnapshot(ScoringVariable variable) {
        try {
            SnapshotDto dto;
            if (!variable.rangos().isEmpty()) {
                List<RangoSnapshotDto> rangos = variable.rangos().stream()
                        .map(r -> new RangoSnapshotDto(
                                r.limiteInferior(), r.limiteSuperior(), r.puntaje(), r.etiqueta()))
                        .toList();
                dto = new SnapshotDto(variable.tipo().name(), rangos, List.of());
            } else {
                List<CategoriaSnapshotDto> categorias = variable.categorias().stream()
                        .map(c -> new CategoriaSnapshotDto(c.categoria(), c.puntaje(), c.etiqueta()))
                        .toList();
                dto = new SnapshotDto(variable.tipo().name(), List.of(), categorias);
            }
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new ScoringModelValidationException(
                    "Error al generar snapshot de la variable: " + variable.nombre());
        }
    }

    // -------------------------------------------------------------------------
    // Records internos para el snapshot JSON
    // -------------------------------------------------------------------------

    private record SnapshotDto(
            String tipo,
            List<RangoSnapshotDto> rangos,
            List<CategoriaSnapshotDto> categorias) {
    }

    private record RangoSnapshotDto(
            java.math.BigDecimal limiteInferior,
            java.math.BigDecimal limiteSuperior,
            int puntaje,
            String etiqueta) {
    }

    private record CategoriaSnapshotDto(String categoria, int puntaje, String etiqueta) {
    }
}
