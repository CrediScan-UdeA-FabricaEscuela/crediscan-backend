package co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaVariableRangeRepository extends JpaRepository<VariableRangeJpaEntity, UUID> {

    List<VariableRangeJpaEntity> findAllByVariableId(UUID variableId);

    void deleteAllByVariableId(UUID variableId);
}
