package co.udea.codefactory.creditscoring.reporting.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.reporting.application.util.PercentageDistributor;
import co.udea.codefactory.creditscoring.reporting.domain.model.HistogramBin;
import co.udea.codefactory.creditscoring.reporting.domain.model.OverallStats;
import co.udea.codefactory.creditscoring.reporting.domain.model.RiskDistributionReport;
import co.udea.codefactory.creditscoring.reporting.domain.model.RiskLevelSummary;
import co.udea.codefactory.creditscoring.reporting.domain.port.in.GetRiskDistributionUseCase;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.RiskDistributionQueryPort;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.RiskDistributionQueryPort.BinAggregate;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.RiskDistributionQueryPort.LevelAggregate;
import lombok.RequiredArgsConstructor;

/**
 * Servicio de aplicación que implementa {@link GetRiskDistributionUseCase}.
 * Resuelve el rango de fechas por defecto (últimos 90 días), valida el rango,
 * consulta los 3 ports de persistencia y construye el reporte de dominio.
 */
@Service
@RequiredArgsConstructor
public class GetRiskDistributionService implements GetRiskDistributionUseCase {

    private static final int DEFAULT_RANGE_DAYS = 90;

    private final RiskDistributionQueryPort queryPort;

    @Override
    @Cacheable(
            value = "riskDistribution",
            key = "#root.target.cacheKey(#desde, #hasta, #tipoEmpleo)"
    )
    public RiskDistributionReport report(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            EmploymentType tipoEmpleo) {

        DateRange range = resolveRange(desde, hasta);
        validateRange(range);
        String empApi = tipoEmpleo == null ? null : tipoEmpleo.apiValue();

        var overall = queryPort.overallStats(range.desde(), range.hasta(), empApi);
        if (overall.total() == 0) {
            return RiskDistributionReport.empty(range.desde(), range.hasta(), empApi);
        }

        var levels = queryPort.distributionByLevel(range.desde(), range.hasta(), empApi);
        var bins = queryPort.histogram(range.desde(), range.hasta(), empApi);

        var tabla = buildTable(levels, overall.total());
        var histograma = fillGaps(bins);
        var stats = new OverallStats(
                overall.avg() == null
                        ? BigDecimal.ZERO.setScale(2)
                        : overall.avg().setScale(2, RoundingMode.HALF_UP),
                overall.stddev() == null
                        ? BigDecimal.ZERO.setScale(2)
                        : overall.stddev().setScale(2, RoundingMode.HALF_UP),
                overall.total()
        );

        return new RiskDistributionReport(tabla, histograma, stats, true,
                range.desde(), range.hasta(), empApi);
    }

    /**
     * Construye la cache key para @Cacheable. Resuelve los defaults antes de hashear
     * para evitar cache miss por parámetros null.
     */
    public String cacheKey(OffsetDateTime desde, OffsetDateTime hasta, EmploymentType tipoEmpleo) {
        DateRange range = resolveRange(desde, hasta);
        return range.desde() + "_" + range.hasta() + "_"
                + (tipoEmpleo == null ? "ALL" : tipoEmpleo.name());
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private DateRange resolveRange(OffsetDateTime desde, OffsetDateTime hasta) {
        OffsetDateTime efectivaHasta = hasta != null ? hasta : OffsetDateTime.now();
        OffsetDateTime efectivaDesde = desde != null
                ? desde
                : efectivaHasta.minusDays(DEFAULT_RANGE_DAYS);
        return new DateRange(efectivaDesde, efectivaHasta);
    }

    private void validateRange(DateRange range) {
        if (range.desde().isAfter(range.hasta())) {
            throw new IllegalArgumentException(
                    "fecha_desde (" + range.desde() + ") no puede ser posterior a fecha_hasta ("
                    + range.hasta() + ")");
        }
    }

    private List<RiskLevelSummary> buildTable(List<LevelAggregate> aggs, long total) {
        List<Long> counts = aggs.stream().map(LevelAggregate::count).toList();
        List<BigDecimal> pcts = PercentageDistributor.largestRemainder(counts, total, 2);
        return IntStream.range(0, aggs.size())
                .mapToObj(i -> new RiskLevelSummary(
                        aggs.get(i).level(),
                        aggs.get(i).count(),
                        pcts.get(i),
                        aggs.get(i).avgScore() == null
                                ? BigDecimal.ZERO.setScale(2)
                                : aggs.get(i).avgScore().setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private List<HistogramBin> fillGaps(List<BinAggregate> bins) {
        Map<Integer, Long> byStart = bins.stream()
                .collect(Collectors.toMap(BinAggregate::binStart, BinAggregate::count));
        return IntStream.range(0, 10)
                .mapToObj(i -> new HistogramBin(i * 10, (i + 1) * 10,
                        byStart.getOrDefault(i * 10, 0L)))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Inner record
    // -------------------------------------------------------------------------

    private record DateRange(OffsetDateTime desde, OffsetDateTime hasta) {}
}
