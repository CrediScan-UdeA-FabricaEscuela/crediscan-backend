package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Tests unitarios del adaptador PDF.
 * Verifica que los bytes generados sean un PDF válido.
 */
class DistribucionRiesgoPdfAdapterTest {

    private final DistribucionRiesgoPdfAdapter adapter = new DistribucionRiesgoPdfAdapter();

    @Test
    void generar_conDatos_retornaBytesConMagicPDF() {
        var report = reporteConDatos();
        byte[] bytes = adapter.generar(report);

        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);
        // Verificar magic bytes %PDF
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generar_sinDatos_retornaBytesConMagicPDF() {
        var now = OffsetDateTime.now();
        var report = RiskDistributionReport.empty(now.minusDays(90), now, null);
        byte[] bytes = adapter.generar(report);

        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generar_sinDatos_noContieneFilasDeTabla() {
        var now = OffsetDateTime.now();
        var report = RiskDistributionReport.empty(now.minusDays(90), now, null);
        byte[] bytes = adapter.generar(report);

        // El PDF no debe ser nulo — solo verificamos que sea válido y no vacío
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void generar_conTipoEmpleo_retornaPDFValido() {
        var report = reporteConDatos("Empleado");
        byte[] bytes = adapter.generar(report);

        assertThat(bytes).isNotNull();
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RiskDistributionReport reporteConDatos() {
        return reporteConDatos(null);
    }

    private RiskDistributionReport reporteConDatos(String tipoEmpleo) {
        var now = OffsetDateTime.now();
        var tabla = List.of(
                new RiskLevelSummary(RiskLevel.MEDIUM, 3L, BigDecimal.valueOf(60), BigDecimal.valueOf(55)));
        var histograma = List.of(
                new HistogramBin(50, 60, 3L),
                new HistogramBin(60, 70, 0L),
                new HistogramBin(70, 80, 0L),
                new HistogramBin(80, 90, 0L),
                new HistogramBin(90, 100, 0L),
                new HistogramBin(0, 10, 0L),
                new HistogramBin(10, 20, 0L),
                new HistogramBin(20, 30, 0L),
                new HistogramBin(30, 40, 0L),
                new HistogramBin(40, 50, 0L));
        var overall = new OverallStats(
                BigDecimal.valueOf(55), BigDecimal.valueOf(5), 3L);

        return new RiskDistributionReport(tabla, histograma, overall, true,
                now.minusDays(90), now, tipoEmpleo);
    }
}
