package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.LatestEvaluationProjection;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.RiskLevelCountProjection;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

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

    @Override
    public List<Evaluation> findAll() {
        return jpaRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "evaluatedAt"
        )).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Map<RiskLevel, Long> countLatestByRiskLevel(OffsetDateTime desde, OffsetDateTime hasta) {
        List<RiskLevelCountProjection> projections =
                jpaRepository.countLatestByRiskLevel(desde, hasta);
        return projections.stream()
                .collect(Collectors.toMap(
                        p -> RiskLevel.valueOf(p.getLevel()),
                        RiskLevelCountProjection::getTotal));
    }

    @Override
    public PagedResult<LatestEvaluationProjection> findLatestPerApplicantInRange(
            RiskLevel nivel, OffsetDateTime desde, OffsetDateTime hasta, PageRequest pageRequest) {
        String nivelStr = nivel != null ? nivel.name() : null;
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        Page<LatestEvaluationProjection> page =
                jpaRepository.findLatestPerApplicantInRange(nivelStr, desde, hasta, pageable);
        return new PagedResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    @Override
    public List<Evaluation> findByApplicantIdOrderByEvaluatedAtDesc(UUID applicantId) {
        return jpaRepository.findByApplicantIdOrderByEvaluatedAtDescIdDesc(applicantId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
