package co.udea.codefactory.creditscoring.creditdecision.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.CreditDecision;
import co.udea.codefactory.creditscoring.creditdecision.domain.port.out.CreditDecisionRepositoryPort;

/**
 * Adaptador de persistencia para decisiones crediticias.
 * Implementa el puerto de salida usando JPA y el mapper de dominio.
 */
@Component
public class CreditDecisionRepositoryAdapter implements CreditDecisionRepositoryPort {

    private final JpaCreditDecisionRepository jpaRepository;
    private final CreditDecisionPersistenceMapper mapper;

    public CreditDecisionRepositoryAdapter(JpaCreditDecisionRepository jpaRepository,
            CreditDecisionPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CreditDecision save(CreditDecision creditDecision) {
        CreditDecisionJpaEntity entity = mapper.toJpaEntity(creditDecision);
        CreditDecisionJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CreditDecision> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CreditDecision> findByEvaluationId(UUID evaluationId) {
        return jpaRepository.findByEvaluationId(evaluationId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEvaluationId(UUID evaluationId) {
        return jpaRepository.existsByEvaluationId(evaluationId);
    }
}
