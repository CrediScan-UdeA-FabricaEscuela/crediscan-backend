package co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaScoringVariableRepository
        extends JpaRepository<ScoringVariableJpaEntity, UUID> {

    Optional<ScoringVariableJpaEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<ScoringVariableJpaEntity> findAllByOrderByNameAsc();

    List<ScoringVariableJpaEntity> findAllByEnabledTrueOrderByNameAsc();
}
