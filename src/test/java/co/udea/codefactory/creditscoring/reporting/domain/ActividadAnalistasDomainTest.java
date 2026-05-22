package co.udea.codefactory.creditscoring.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalista;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.DistribucionDecisiones;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.EstadisticasEquipo;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.FiltroActividad;

/**
 * Tests unitarios para los records de dominio de HU-017.
 * Verifica: FiltroActividad lanza en rango inválido,
 * ActividadAnalistasReporte.empty() retorna hasData=false.
 */
class ActividadAnalistasDomainTest {

    @Test
    void filtro_rangoValido_seCreaSinError() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(30);
        OffsetDateTime hasta = OffsetDateTime.now();
        FiltroActividad filtro = new FiltroActividad(desde, hasta);
        assertThat(filtro.desde()).isEqualTo(desde);
        assertThat(filtro.hasta()).isEqualTo(hasta);
    }

    @Test
    void filtro_desdeMayorQueHasta_lanzaIllegalArgumentException() {
        OffsetDateTime desde = OffsetDateTime.now();
        OffsetDateTime hasta = desde.minusDays(1);
        assertThatThrownBy(() -> new FiltroActividad(desde, hasta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desde");
    }

    @Test
    void empty_retornaHasDataFalse() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(30);
        OffsetDateTime hasta = OffsetDateTime.now();
        ActividadAnalistasReporte reporte = ActividadAnalistasReporte.empty(desde, hasta);
        assertThat(reporte.hasData()).isFalse();
        assertThat(reporte.analistas()).isEmpty();
        assertThat(reporte.estadisticasEquipo().numAnalistas()).isZero();
    }

    // =========================================================================
    // FIX-1: EstadisticasEquipo debe incluir tasaAprobacionEquipo (CA4-017)
    // =========================================================================

    @Test
    void estadisticasEquipo_tieneCampoTasaAprobacionEquipo() {
        // Verifica que el record EstadisticasEquipo expone el campo tasaAprobacionEquipo
        java.math.BigDecimal tasa = new java.math.BigDecimal("75.00");
        EstadisticasEquipo estadisticas = new EstadisticasEquipo(100L, 2.5, 0.5, 4, false, tasa);
        assertThat(estadisticas.tasaAprobacionEquipo())
                .isNotNull()
                .isEqualByComparingTo(new java.math.BigDecimal("75.00"));
    }

    @Test
    void estadisticasEquipo_empty_tasaAprobacionEquipoEsCero() {
        // empty() retorna tasaAprobacionEquipo = 0
        OffsetDateTime desde = OffsetDateTime.now().minusDays(30);
        OffsetDateTime hasta = OffsetDateTime.now();
        ActividadAnalistasReporte reporte = ActividadAnalistasReporte.empty(desde, hasta);
        assertThat(reporte.estadisticasEquipo().tasaAprobacionEquipo())
                .isNotNull()
                .isEqualByComparingTo(java.math.BigDecimal.ZERO.setScale(2));
    }

    // =========================================================================
    // FIX-2: ActividadAnalista debe incluir nombre (CA1-017)
    // =========================================================================

    @Test
    void actividadAnalista_tieneCampoNombre() {
        // Verifica que el record ActividadAnalista expone el campo nombre
        DistribucionDecisiones dist = new DistribucionDecisiones(
                5, 3, 1, 1,
                new java.math.BigDecimal("50.00"),
                new java.math.BigDecimal("30.00"),
                new java.math.BigDecimal("10.00"),
                new java.math.BigDecimal("10.00"));
        ActividadAnalista analista = new ActividadAnalista("user_ana", "user_ana", 10, dist, 2.5, false);
        assertThat(analista.nombre())
                .isNotNull()
                .isEqualTo("user_ana");
    }
}
