package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.DecisionFilterValue;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationStats;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.LatestEvaluationProjection;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.RiskLevelCountProjection;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.projection.EvaluationSearchItemProjection;
import co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence.projection.EvaluationStatsProjection;
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

    @Override
    public PagedResult<EvaluationSearchItem> search(EvaluationSearchCriteria criteria, PageRequest pageRequest) {
        SplittedDecisions split = splitDecisions(criteria.decisiones());
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        Page<EvaluationSearchItemProjection> page = jpaRepository.searchEvaluations(
                criteria.fechaDesde(),
                criteria.fechaHasta(),
                toCsv(criteria.niveles(), RiskLevel::name),
                criteria.puntajeMin(),
                criteria.puntajeMax(),
                split.realesCsv(),
                split.includeSinDecision(),
                criteria.analista(),
                pageable);
        List<EvaluationSearchItem> items = page.getContent().stream()
                .map(this::toSearchItem)
                .toList();
        return new PagedResult<>(items, page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }

    @Override
    public EvaluationStats stats(EvaluationSearchCriteria criteria) {
        SplittedDecisions split = splitDecisions(criteria.decisiones());
        EvaluationStatsProjection proj = jpaRepository.statsByCriteria(
                criteria.fechaDesde(),
                criteria.fechaHasta(),
                toCsv(criteria.niveles(), RiskLevel::name),
                criteria.puntajeMin(),
                criteria.puntajeMax(),
                split.realesCsv(),
                split.includeSinDecision(),
                criteria.analista());
        Map<RiskLevel, Long> distribution = new EnumMap<>(RiskLevel.class);
        for (RiskLevel level : RiskLevel.values()) {
            distribution.put(level, 0L);
        }
        if (proj.getTotal() > 0) {
            distribution.put(RiskLevel.VERY_LOW, proj.getCountVeryLow());
            distribution.put(RiskLevel.LOW, proj.getCountLow());
            distribution.put(RiskLevel.MEDIUM, proj.getCountMedium());
            distribution.put(RiskLevel.HIGH, proj.getCountHigh());
            distribution.put(RiskLevel.VERY_HIGH, proj.getCountVeryHigh());
            distribution.put(RiskLevel.REJECTED, proj.getCountRejected());
        }
        return new EvaluationStats(
                proj.getTotal(),
                proj.getAvgScore(),
                distribution);
    }

    @Override
    public long countByCriteria(EvaluationSearchCriteria criteria) {
        SplittedDecisions split = splitDecisions(criteria.decisiones());
        return jpaRepository.countByCriteria(
                criteria.fechaDesde(),
                criteria.fechaHasta(),
                toCsv(criteria.niveles(), RiskLevel::name),
                criteria.puntajeMin(),
                criteria.puntajeMax(),
                split.realesCsv(),
                split.includeSinDecision(),
                criteria.analista());
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /** Separa decisiones reales del valor sintético SIN_DECISION. */
    private record SplittedDecisions(String realesCsv, boolean includeSinDecision) {}

    private SplittedDecisions splitDecisions(List<DecisionFilterValue> decisiones) {
        if (decisiones == null) {
            return new SplittedDecisions(null, false);
        }
        boolean includeSin = decisiones.stream().anyMatch(DecisionFilterValue::isSinDecision);
        List<String> reales = decisiones.stream()
                .filter(d -> !d.isSinDecision())
                .map(Enum::name)
                .toList();
        String realesCsv = reales.isEmpty() ? null : String.join(",", reales);
        return new SplittedDecisions(realesCsv, includeSin);
    }

    /** Convierte lista a CSV string; retorna null si la lista es null o vacía. */
    private <T> String toCsv(List<T> list, java.util.function.Function<T, String> mapper) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().map(mapper).collect(Collectors.joining(","));
    }

    /** Convierte proyección de búsqueda al tipo de dominio. */
    private EvaluationSearchItem toSearchItem(EvaluationSearchItemProjection p) {
        // Hibernate proyecta TIMESTAMP WITH TIME ZONE como Instant en native queries.
        OffsetDateTime evaluatedAt = p.getEvaluatedAt() != null
                ? p.getEvaluatedAt().atOffset(ZoneOffset.UTC)
                : null;
        return new EvaluationSearchItem(
                p.getEvaluationId(),
                p.getApplicantId(),
                p.getApplicantName(),
                evaluatedAt,
                p.getScore(),
                RiskLevel.valueOf(p.getRiskLevel()),
                p.getDecisionStatus(),
                p.getAnalista());
    }
}
