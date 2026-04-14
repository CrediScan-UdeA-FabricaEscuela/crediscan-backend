package co.udea.codefactory.creditscoring.scoring.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaVariableCategoryRepository
        extends JpaRepository<VariableCategoryJpaEntity, UUID> {

    List<VariableCategoryJpaEntity> findAllByVariableId(UUID variableId);

    void deleteAllByVariableId(UUID variableId);
}
