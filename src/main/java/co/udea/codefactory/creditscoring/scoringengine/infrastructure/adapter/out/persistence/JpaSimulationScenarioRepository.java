package co.udea.codefactory.creditscoring.scoringengine.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface JpaSimulationScenarioRepository
        extends JpaRepository<SimulationScenarioJpaEntity, UUID> {

    List<SimulationScenarioJpaEntity> findByModelIdOrderByCreatedAtDesc(UUID modelId);
}
