package co.udea.codefactory.creditscoring.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.reporting.domain.model.HistogramBin;
import co.udea.codefactory.creditscoring.reporting.domain.model.OverallStats;
import co.udea.codefactory.creditscoring.reporting.domain.model.RiskDistributionReport;
import co.udea.codefactory.creditscoring.reporting.domain.model.RiskLevelSummary;

/**
 * Tests unitarios de invariantes de los records del dominio.
 */
class RiskDistributionReportDomainTest {

    // =========================================================================
    // RiskLevelSummary
    // =========================================================================

    @Test
    void riskLevelSummary_cuandoCountNegativo_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new RiskLevelSummary(RiskLevel.LOW, -1L, BigDecimal.TEN, BigDecimal.valueOf(75)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count");
    }

    @Test
    void riskLevelSummary_cuandoPorcentajeMayorA100_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new RiskLevelSummary(RiskLevel.LOW, 1L, BigDecimal.valueOf(101), BigDecimal.valueOf(75)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("percentage");
    }

    @Test
    void riskLevelSummary_cuandoPorcentajeNegativo_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new RiskLevelSummary(RiskLevel.LOW, 1L, BigDecimal.valueOf(-1), BigDecimal.valueOf(75)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("percentage");
    }

    @Test
    void riskLevelSummary_cuandoLevelNull_lanzaExcepcion() {
        assertThatThrownBy(() ->
                new RiskLevelSummary(null, 1L, BigDecimal.TEN, BigDecimal.valueOf(75)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void riskLevelSummary_valoresValidos_creaCorrectamente() {
        var summary = new RiskLevelSummary(RiskLevel.MEDIUM, 5L,
                BigDecimal.valueOf(33.33), BigDecimal.valueOf(55.0));
        assertThat(summary.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(summary.count()).isEqualTo(5L);
    }

    // =========================================================================
    // HistogramBin
    // =========================================================================

    @Test
    void histogramBin_cuandoBinStartNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> new HistogramBin(-1, 10, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void histogramBin_cuandoBinEndMayorA100_lanzaExcepcion() {
        assertThatThrownBy(() -> new HistogramBin(90, 101, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void histogramBin_cuandoBinEndMenorOIgualABinStart_lanzaExcepcion() {
        assertThatThrownBy(() -> new HistogramBin(50, 50, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HistogramBin(50, 40, 0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void histogramBin_cuandoCountNegativo_lanzaExcepcion() {
        assertThatThrownBy(() -> new HistogramBin(0, 10, -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void histogramBin_empty10Bins_retornaExactamente10Bins() {
        List<HistogramBin> bins = HistogramBin.empty10Bins();
        assertThat(bins).hasSize(10);
        assertThat(bins.get(0).binStart()).isEqualTo(0);
        assertThat(bins.get(0).binEnd()).isEqualTo(10);
        assertThat(bins.get(9).binStart()).isEqualTo(90);
        assertThat(bins.get(9).binEnd()).isEqualTo(100);
        assertThat(bins).allMatch(b -> b.count() == 0L);
    }

    // =========================================================================
    // OverallStats
    // =========================================================================

    @Test
    void overallStats_zero_retornaValoresEnCero() {
        OverallStats zero = OverallStats.zero();
        assertThat(zero.averageScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.stdDev()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.totalEvaluations()).isEqualTo(0L);
    }

    @Test
    void overallStats_zero_tieneScale2() {
        OverallStats zero = OverallStats.zero();
        assertThat(zero.averageScore().scale()).isEqualTo(2);
        assertThat(zero.stdDev().scale()).isEqualTo(2);
    }

    // =========================================================================
    // RiskDistributionReport
    // =========================================================================

    @Test
    void riskDistributionReport_empty_tieneHasDataFalse() {
        var now = OffsetDateTime.now();
        var report = RiskDistributionReport.empty(now, now.plusDays(1), null);
        assertThat(report.hasData()).isFalse();
        assertThat(report.tabla()).isEmpty();
        assertThat(report.histograma()).hasSize(10);
        assertThat(report.overall()).isEqualTo(OverallStats.zero());
    }

    @Test
    void riskDistributionReport_empty_conservaTipoEmpleo() {
        var now = OffsetDateTime.now();
        var report = RiskDistributionReport.empty(now, now.plusDays(1), "Empleado");
        assertThat(report.tipoEmpleo()).isEqualTo("Empleado");
    }
}
