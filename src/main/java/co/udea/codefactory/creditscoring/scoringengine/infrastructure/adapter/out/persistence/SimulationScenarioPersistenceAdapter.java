package co.udea.codefactory.creditscoring.scoringengine.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.udea.codefactory.creditscoring.scoringengine.domain.model.SimulationScenario;
import co.udea.codefactory.creditscoring.scoringengine.domain.port.out.SimulationScenarioRepositoryPort;

@Component
public class SimulationScenarioPersistenceAdapter implements SimulationScenarioRepositoryPort {

    private static final TypeReference<Map<String, BigDecimal>> MAP_TYPE =
            new TypeReference<>() {};

    private final JpaSimulationScenarioRepository jpaRepo;
    private final ObjectMapper objectMapper;

    public SimulationScenarioPersistenceAdapter(
            JpaSimulationScenarioRepository jpaRepo,
            ObjectMapper objectMapper) {
        this.jpaRepo = jpaRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public SimulationScenario save(SimulationScenario escenario) {
        return toDomain(jpaRepo.save(toEntity(escenario)));
    }

    @Override
    public Optional<SimulationScenario> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<SimulationScenario> findByModeloId(UUID modeloId) {
        return jpaRepo.findByModelIdOrderByCreatedAtDesc(modeloId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepo.deleteById(id);
    }

    // -------------------------------------------------------------------------

    private SimulationScenarioJpaEntity toEntity(SimulationScenario escenario) {
        SimulationScenarioJpaEntity e = new SimulationScenarioJpaEntity();
        e.setId(escenario.id());
        e.setModelId(escenario.modeloId());
        e.setName(escenario.nombre());
        e.setDescription(escenario.descripcion());
        e.setValuesSnapshot(serializarValores(escenario.valoresVariables()));
        e.setCreatedAt(escenario.fechaCreacion());
        e.setCreatedBy(escenario.creadoPor() != null ? escenario.creadoPor() : "system");
        return e;
    }

    private SimulationScenario toDomain(SimulationScenarioJpaEntity e) {
        return SimulationScenario.rehydrate(
                e.getId(),
                e.getModelId(),
                e.getName(),
                e.getDescription(),
                deserializarValores(e.getValuesSnapshot()),
                e.getCreatedAt(),
                e.getCreatedBy());
    }

    private String serializarValores(Map<String, BigDecimal> valores) {
        try {
            return objectMapper.writeValueAsString(valores);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Error serializando valores del escenario", ex);
        }
    }

    private Map<String, BigDecimal> deserializarValores(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Error deserializando valores del escenario", ex);
        }
    }
}
