package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.RiskDistributionQueryPort;
import lombok.RequiredArgsConstructor;

/**
 * Adaptador de persistencia que implementa {@link RiskDistributionQueryPort}
 * delegando a {@link JpaRiskDistributionRepository}.
 */
@Component
@RequiredArgsConstructor
public class RiskDistributionQueryAdapter implements RiskDistributionQueryPort {

    private final JpaRiskDistributionRepository repo;

    @Override
    public List<LevelAggregate> distributionByLevel(
            OffsetDateTime desde, OffsetDateTime hasta, String tipoEmpleo) {
        return repo.distributionByLevel(desde, hasta, tipoEmpleo).stream()
                .map(p -> new LevelAggregate(
                        RiskLevel.valueOf(p.getLevel()),
                        p.getCnt(),
                        p.getAvgScore() == null ? BigDecimal.ZERO : p.getAvgScore()))
                .toList();
    }

    @Override
    public List<BinAggregate> histogram(
            OffsetDateTime desde, OffsetDateTime hasta, String tipoEmpleo) {
        return repo.histogram(desde, hasta, tipoEmpleo).stream()
                .map(p -> new BinAggregate(p.getBinStart(), p.getCnt()))
                .toList();
    }

    @Override
    public OverallAggregate overallStats(
            OffsetDateTime desde, OffsetDateTime hasta, String tipoEmpleo) {
        OverallStatsProjection p = repo.overallStats(desde, hasta, tipoEmpleo);
        return new OverallAggregate(p.getTotal(), p.getAvg(), p.getStddev());
    }
}
