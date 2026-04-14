package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaKnockoutRuleRepository extends JpaRepository<KnockoutRuleJpaEntity, UUID> {

    List<KnockoutRuleJpaEntity> findByModelIdOrderByPriorityAsc(UUID modelId);

    List<KnockoutRuleJpaEntity> findByModelIdAndEnabledTrueOrderByPriorityAsc(UUID modelId);

    int countByModelIdAndEnabledTrue(UUID modelId);
}
