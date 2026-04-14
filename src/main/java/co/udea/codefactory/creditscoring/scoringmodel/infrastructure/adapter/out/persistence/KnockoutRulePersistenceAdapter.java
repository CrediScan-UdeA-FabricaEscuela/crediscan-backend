package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutOperator;
import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;
import co.udea.codefactory.creditscoring.scoringmodel.domain.port.out.KnockoutRuleRepositoryPort;

@Component
public class KnockoutRulePersistenceAdapter implements KnockoutRuleRepositoryPort {

    private final JpaKnockoutRuleRepository jpaRepo;

    public KnockoutRulePersistenceAdapter(JpaKnockoutRuleRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public KnockoutRule save(KnockoutRule rule) {
        KnockoutRuleJpaEntity entity = toEntity(rule);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setCreatedBy(currentUser());
        return toDomain(jpaRepo.save(entity));
    }

    @Override
    public KnockoutRule update(KnockoutRule rule) {
        KnockoutRuleJpaEntity existing = jpaRepo.findById(rule.id()).orElseThrow();
        existing.setModelId(rule.modeloId());
        existing.setField(rule.campo());
        existing.setOperator(rule.operador().name());
        existing.setThresholdValue(rule.umbral().toPlainString());
        existing.setDescription(rule.mensaje());
        existing.setName(rule.campo());
        existing.setPriority(rule.prioridad());
        existing.setEnabled(rule.activa());
        existing.setUpdatedAt(OffsetDateTime.now());
        existing.setUpdatedBy(currentUser());
        return toDomain(jpaRepo.save(existing));
    }

    @Override
    public Optional<KnockoutRule> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<KnockoutRule> findByModeloId(UUID modeloId) {
        return jpaRepo.findByModelIdOrderByPriorityAsc(modeloId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<KnockoutRule> findActivasByModeloId(UUID modeloId) {
        return jpaRepo.findByModelIdAndEnabledTrueOrderByPriorityAsc(modeloId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public int countActivasByModeloId(UUID modeloId) {
        return jpaRepo.countByModelIdAndEnabledTrue(modeloId);
    }

    // -------------------------------------------------------------------------

    private KnockoutRuleJpaEntity toEntity(KnockoutRule rule) {
        KnockoutRuleJpaEntity e = new KnockoutRuleJpaEntity();
        e.setId(rule.id());
        e.setModelId(rule.modeloId());
        e.setField(rule.campo());
        e.setOperator(rule.operador().name());
        e.setThresholdValue(rule.umbral().toPlainString());
        e.setDescription(rule.mensaje());
        // 'name' es NOT NULL — usamos el campo como nombre corto
        e.setName(rule.campo());
        e.setPriority(rule.prioridad());
        e.setEnabled(rule.activa());
        return e;
    }

    private KnockoutRule toDomain(KnockoutRuleJpaEntity e) {
        return KnockoutRule.rehydrate(
                e.getId(),
                e.getModelId(),
                e.getField(),
                KnockoutOperator.valueOf(e.getOperator()),
                e.getThresholdValueAsBigDecimal(),
                e.getDescription() != null ? e.getDescription() : e.getName(),
                e.getPriority(),
                e.isEnabled());
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
