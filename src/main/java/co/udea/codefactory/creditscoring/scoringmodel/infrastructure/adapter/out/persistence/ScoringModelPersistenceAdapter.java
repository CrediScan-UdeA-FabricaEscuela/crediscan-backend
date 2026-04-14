package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelStatus;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ModelVariable;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.ScoringModel;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.ScoringModelRepositoryPort;

@Component
public class ScoringModelPersistenceAdapter implements ScoringModelRepositoryPort {

    private final JpaScoringModelRepository modelRepo;
    private final JpaModelVariableRepository variableRepo;

    @Autowired
    public ScoringModelPersistenceAdapter(
            JpaScoringModelRepository modelRepo,
            JpaModelVariableRepository variableRepo) {
        this.modelRepo = modelRepo;
        this.variableRepo = variableRepo;
    }

    @Override
    public ScoringModel save(ScoringModel modelo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        String usuario = currentUsername();

        ScoringModelJpaEntity entity = toEntity(modelo);
        entity.setCreatedAt(ahora);
        entity.setUpdatedAt(ahora);
        entity.setCreatedBy(usuario);
        entity.setUpdatedBy(usuario);
        modelRepo.save(entity);

        guardarVariables(modelo.variables(), modelo.id(), ahora, usuario);
        return modelo;
    }

    @Override
    public ScoringModel update(ScoringModel modelo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        String usuario = currentUsername();

        ScoringModelJpaEntity entity = modelRepo.findById(modelo.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Modelo no encontrado al intentar actualizar: " + modelo.id()));
        entity.setName(modelo.nombre());
        entity.setDescription(modelo.descripcion());
        entity.setStatus(modelo.estado().name());
        entity.setActivatedAt(modelo.fechaActivacion());
        entity.setUpdatedAt(ahora);
        entity.setUpdatedBy(usuario);
        modelRepo.save(entity);

        // Estrategia delete-and-recreate para las variables del agregado
        variableRepo.deleteAllByModelId(modelo.id());
        guardarVariables(modelo.variables(), modelo.id(), ahora, usuario);

        return modelo;
    }

    @Override
    public Optional<ScoringModel> findById(UUID id) {
        return modelRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<ScoringModel> findAll() {
        return modelRepo.findAllByOrderByVersionDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ScoringModel> findActive() {
        return modelRepo.findByStatus(ModelStatus.ACTIVE.name()).map(this::toDomain);
    }

    @Override
    public int maxVersion() {
        return modelRepo.findMaxVersion();
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return modelRepo.existsByName(nombre);
    }

    // -------------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------------

    private void guardarVariables(
            List<ModelVariable> variables, UUID modeloId, OffsetDateTime ahora, String usuario) {
        variables.forEach(mv -> {
            ModelVariableJpaEntity ve = new ModelVariableJpaEntity();
            ve.setId(mv.id() != null ? mv.id() : UUID.randomUUID());
            ve.setModelId(modeloId);
            ve.setVariableId(mv.variableId());
            ve.setWeight(mv.peso());
            ve.setRangesSnapshot(mv.rangosSnapshot());
            ve.setCreatedAt(ahora);
            ve.setUpdatedAt(ahora);
            ve.setCreatedBy(usuario);
            ve.setUpdatedBy(usuario);
            variableRepo.save(ve);
        });
    }

    private ScoringModel toDomain(ScoringModelJpaEntity entity) {
        List<ModelVariable> variables = variableRepo.findAllByModelId(entity.getId()).stream()
                .map(mv -> new ModelVariable(
                        mv.getId(),
                        entity.getId(),
                        mv.getVariableId(),
                        mv.getWeight(),
                        mv.getRangesSnapshot()))
                .toList();

        return ScoringModel.rehydrate(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getVersion(),
                ModelStatus.fromValor(entity.getStatus()),
                variables,
                entity.getCreatedAt(),
                entity.getActivatedAt());
    }

    private ScoringModelJpaEntity toEntity(ScoringModel modelo) {
        ScoringModelJpaEntity entity = new ScoringModelJpaEntity();
        entity.setId(modelo.id());
        entity.setName(modelo.nombre());
        entity.setDescription(modelo.descripcion());
        entity.setVersion(modelo.version());
        entity.setStatus(modelo.estado().name());
        entity.setActivatedAt(modelo.fechaActivacion());
        return entity;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "system";
    }
}
