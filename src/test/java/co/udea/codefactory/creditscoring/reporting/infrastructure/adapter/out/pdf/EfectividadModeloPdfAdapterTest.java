package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.creditdecision.domain.model.DecisionStatus;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.CasoOverride;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.CeldaMatriz;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.EfectividadModeloReporte;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.IndicadoresEfectividad;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.MatrizConfusion;
import co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.pdf.efectividad.EfectividadModeloPdfAdapter;

/**
 * Tests unitarios del adaptador PDF de efectividad del modelo.
 * Sin Spring — instancia directa.
 */
class EfectividadModeloPdfAdapterTest {

    private final EfectividadModeloPdfAdapter adapter = new EfectividadModeloPdfAdapter();

    // =========================================================================
    // Reporte con datos → bytes > 0 y magic bytes %PDF
    // =========================================================================

    @Test
    void reporteConDatos_retornaBytesNoPDF() {
        EfectividadModeloReporte reporte = buildReporteConDatos();
        byte[] bytes = adapter.generar(reporte);

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void reporteConDatos_tamañoMayorACero() {
        byte[] bytes = adapter.generar(buildReporteConDatos());
        assertThat(bytes.length).isGreaterThan(100);
    }

    // =========================================================================
    // Reporte sin datos → PDF válido con mensaje
    // =========================================================================

    @Test
    void reporteSinDatos_retornaPdfValido() {
        EfectividadModeloReporte reporte = EfectividadModeloReporte.empty(
                OffsetDateTime.now().minusDays(30), OffsetDateTime.now(), null);
        byte[] bytes = adapter.generar(reporte);

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private EfectividadModeloReporte buildReporteConDatos() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(30);
        OffsetDateTime hasta = OffsetDateTime.now();

        List<CeldaMatriz> celdas = List.of(
                new CeldaMatriz(RiskLevel.VERY_LOW, DecisionStatus.APPROVED, 10),
                new CeldaMatriz(RiskLevel.HIGH, DecisionStatus.REJECTED, 5));

        IndicadoresEfectividad indicadores = new IndicadoresEfectividad(
                new BigDecimal("80.00"), new BigDecimal("5.00"),
                new BigDecimal("0.00"), 100L);

        return new EfectividadModeloReporte(
                new MatrizConfusion(celdas),
                indicadores,
                List.of(new CasoOverride(RiskLevel.HIGH, DecisionStatus.APPROVED, 5)),
                true, desde, hasta, null);
    }
}
