package co.udea.codefactory.creditscoring.scoringmodel.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaModelVariableRepository
        extends JpaRepository<ModelVariableJpaEntity, UUID> {

    List<ModelVariableJpaEntity> findAllByModelId(UUID modelId);

    void deleteAllByModelId(UUID modelId);
}
