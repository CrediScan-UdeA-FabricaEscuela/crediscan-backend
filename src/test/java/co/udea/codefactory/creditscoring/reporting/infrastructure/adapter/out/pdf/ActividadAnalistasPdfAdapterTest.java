package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalista;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.DistribucionDecisiones;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.EstadisticasEquipo;
import co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.pdf.analistas.ActividadAnalistasPdfAdapter;

/**
 * Tests unitarios del adaptador PDF de actividad de analistas.
 * Sin Spring — instancia directa.
 */
class ActividadAnalistasPdfAdapterTest {

    private final ActividadAnalistasPdfAdapter adapter = new ActividadAnalistasPdfAdapter();

    @Test
    void reporteConDatos_retornaPdfConMagicBytes() {
        byte[] bytes = adapter.generar(buildReporteConDatos());
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void reporteSinDatos_retornaPdfValido() {
        ActividadAnalistasReporte vacio = ActividadAnalistasReporte.empty(
                OffsetDateTime.now().minusDays(30), OffsetDateTime.now());
        byte[] bytes = adapter.generar(vacio);
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
    }

    private ActividadAnalistasReporte buildReporteConDatos() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(30);
        OffsetDateTime hasta = OffsetDateTime.now();

        DistribucionDecisiones dist = new DistribucionDecisiones(
                8, 1, 0, 1, new BigDecimal("80.00"),
                new BigDecimal("10.00"), BigDecimal.ZERO.setScale(2),
                new BigDecimal("10.00"));

        ActividadAnalista analista = new ActividadAnalista("user_ana", "user_ana", 10, dist, 2.5, false);

        EstadisticasEquipo equipo = new EstadisticasEquipo(10L, 2.5, 0.0, 1, true, BigDecimal.ZERO.setScale(2));

        return new ActividadAnalistasReporte(List.of(analista), equipo, true, desde, hasta);
    }
}
