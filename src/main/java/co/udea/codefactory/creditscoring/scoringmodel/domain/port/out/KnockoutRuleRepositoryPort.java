package co.udea.codefactory.creditscoring.scoringmodel.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.domain.model.KnockoutRule;

public interface KnockoutRuleRepositoryPort {

    KnockoutRule save(KnockoutRule rule);

    KnockoutRule update(KnockoutRule rule);

    Optional<KnockoutRule> findById(UUID id);

    /** Retorna todas las reglas de un modelo, ordenadas por prioridad ascendente. */
    List<KnockoutRule> findByModeloId(UUID modeloId);

    /** Retorna solo las reglas activas de un modelo, ordenadas por prioridad ascendente. */
    List<KnockoutRule> findActivasByModeloId(UUID modeloId);

    void deleteById(UUID id);

    /** Cuenta las reglas activas del modelo. */
    int countActivasByModeloId(UUID modeloId);
}
