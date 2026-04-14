package co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.scoring.domain.model.ScoringVariable;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableCategory;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableRange;
import co.udea.codefactory.creditscoring.scoring.domain.model.VariableType;
import co.udea.codefactory.creditscoring.scoring.domain.port.out.ScoringVariableRepositoryPort;

@Component
public class ScoringVariablePersistenceAdapter implements ScoringVariableRepositoryPort {

    private final JpaScoringVariableRepository variableRepo;
    private final JpaVariableRangeRepository rangeRepo;
    private final JpaVariableCategoryRepository categoryRepo;

    @Autowired
    public ScoringVariablePersistenceAdapter(
            JpaScoringVariableRepository variableRepo,
            JpaVariableRangeRepository rangeRepo,
            JpaVariableCategoryRepository categoryRepo) {
        this.variableRepo = variableRepo;
        this.rangeRepo = rangeRepo;
        this.categoryRepo = categoryRepo;
    }

    @Override
    public ScoringVariable save(ScoringVariable variable) {
        OffsetDateTime ahora = OffsetDateTime.now();
        String usuario = currentUsername();

        ScoringVariableJpaEntity entity = toEntity(variable);
        entity.setCreatedAt(ahora);
        entity.setUpdatedAt(ahora);
        entity.setCreatedBy(usuario);
        entity.setUpdatedBy(usuario);
        variableRepo.save(entity);

        guardarRangos(variable.rangos(), variable.id(), ahora);
        guardarCategorias(variable.categorias(), variable.id(), ahora);

        return variable;
    }

    @Override
    public ScoringVariable update(ScoringVariable variable) {
        OffsetDateTime ahora = OffsetDateTime.now();
        String usuario = currentUsername();

        ScoringVariableJpaEntity entity = variableRepo.findById(variable.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Variable no encontrada al intentar actualizar: " + variable.id()));
        entity.setName(variable.nombre());
        entity.setDescription(variable.descripcion());
        entity.setPeso(variable.peso());
        entity.setEnabled(variable.activa());
        entity.setUpdatedAt(ahora);
        entity.setUpdatedBy(usuario);
        variableRepo.save(entity);

        // Estrategia delete-and-recreate para las colecciones del agregado
        rangeRepo.deleteAllByVariableId(variable.id());
        categoryRepo.deleteAllByVariableId(variable.id());

        guardarRangos(variable.rangos(), variable.id(), ahora);
        guardarCategorias(variable.categorias(), variable.id(), ahora);

        return variable;
    }

    @Override
    public Optional<ScoringVariable> findById(UUID id) {
        return variableRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ScoringVariable> findByNombre(String nombre) {
        return variableRepo.findByNameIgnoreCase(nombre).map(this::toDomain);
    }

    @Override
    public List<ScoringVariable> findAll() {
        return variableRepo.findAllByOrderByNameAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ScoringVariable> findAllActivas() {
        return variableRepo.findAllByEnabledTrueOrderByNameAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return variableRepo.existsByNameIgnoreCase(nombre);
    }

    // -------------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------------

    private void guardarRangos(List<VariableRange> rangos, UUID variableId, OffsetDateTime ahora) {
        rangos.forEach(r -> {
            VariableRangeJpaEntity re = new VariableRangeJpaEntity();
            re.setId(r.id() != null ? r.id() : UUID.randomUUID());
            re.setVariableId(variableId);
            re.setMinValue(r.limiteInferior());
            re.setMaxValue(r.limiteSuperior());
            re.setScore(r.puntaje());
            re.setLabel(r.etiqueta());
            re.setCreatedAt(ahora);
            rangeRepo.save(re);
        });
    }

    private void guardarCategorias(
            List<VariableCategory> categorias, UUID variableId, OffsetDateTime ahora) {
        categorias.forEach(c -> {
            VariableCategoryJpaEntity ce = new VariableCategoryJpaEntity();
            ce.setId(c.id() != null ? c.id() : UUID.randomUUID());
            ce.setVariableId(variableId);
            ce.setCategoryValue(c.categoria());
            ce.setScore(c.puntaje());
            ce.setLabel(c.etiqueta());
            ce.setCreatedAt(ahora);
            categoryRepo.save(ce);
        });
    }

    private ScoringVariable toDomain(ScoringVariableJpaEntity entity) {
        List<VariableRange> rangos = rangeRepo.findAllByVariableId(entity.getId()).stream()
                .map(r -> new VariableRange(
                        r.getId(),
                        entity.getId(),
                        r.getMinValue(),
                        r.getMaxValue(),
                        r.getScore(),
                        r.getLabel()))
                .toList();

        List<VariableCategory> categorias = categoryRepo.findAllByVariableId(entity.getId()).stream()
                .map(c -> new VariableCategory(
                        c.getId(),
                        entity.getId(),
                        c.getCategoryValue(),
                        c.getScore(),
                        c.getLabel()))
                .toList();

        return ScoringVariable.rehydrate(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                VariableType.fromValor(entity.getVariableType()),
                entity.getPeso(),
                entity.isEnabled(),
                rangos,
                categorias);
    }

    private ScoringVariableJpaEntity toEntity(ScoringVariable variable) {
        ScoringVariableJpaEntity entity = new ScoringVariableJpaEntity();
        entity.setId(variable.id());
        entity.setName(variable.nombre());
        entity.setDescription(variable.descripcion());
        entity.setVariableType(variable.tipo().name());
        entity.setPeso(variable.peso());
        entity.setEnabled(variable.activa());
        return entity;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "system";
    }
}
