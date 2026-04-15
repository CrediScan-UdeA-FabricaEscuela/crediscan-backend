package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;

/**
 * Adaptador de persistencia para evaluaciones.
 * Implementa el puerto de salida usando JPA y el mapper de dominio.
 */
@Component
public class EvaluationRepositoryAdapter implements EvaluationRepositoryPort {

    private final JpaEvaluationRepository jpaRepository;
    private final EvaluationPersistenceMapper mapper;

    public EvaluationRepositoryAdapter(JpaEvaluationRepository jpaRepository,
            EvaluationPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Evaluation save(Evaluation evaluation) {
        EvaluationJpaEntity entity = mapper.toJpaEntity(evaluation);
        EvaluationJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Evaluation> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByApplicantIdAndEvaluatedAtAfter(UUID applicantId, OffsetDateTime since) {
        return jpaRepository.existsByApplicantIdAndEvaluatedAtAfter(applicantId, since);
    }
}
