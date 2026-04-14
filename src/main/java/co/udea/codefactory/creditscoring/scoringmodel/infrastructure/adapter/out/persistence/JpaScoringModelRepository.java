package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaScoringModelRepository
        extends JpaRepository<ScoringModelJpaEntity, UUID> {

    Optional<ScoringModelJpaEntity> findByStatus(String status);

    List<ScoringModelJpaEntity> findAllByOrderByVersionDesc();

    boolean existsByName(String name);

    @Query("SELECT COALESCE(MAX(m.version), 0) FROM ScoringModelJpaEntity m")
    int findMaxVersion();
}
