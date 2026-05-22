package co.udea.codefactory.creditscoring.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.EfectividadModeloReporte;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.FiltroEfectividad;

/**
 * Tests unitarios para los records de dominio de HU-016.
 * Verifica: FiltroEfectividad lanza en rango inválido,
 * EfectividadModeloReporte.empty() retorna hasData=false.
 */
class EfectividadModeloDomainTest {

    @Test
    void filtro_rangoValido_seCreaSinError() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(10);
        OffsetDateTime hasta = OffsetDateTime.now();
        FiltroEfectividad filtro = new FiltroEfectividad(desde, hasta, null);
        assertThat(filtro.desde()).isEqualTo(desde);
        assertThat(filtro.hasta()).isEqualTo(hasta);
    }

    @Test
    void filtro_desdeIgualHasta_seCreaSinError() {
        OffsetDateTime t = OffsetDateTime.now();
        FiltroEfectividad filtro = new FiltroEfectividad(t, t, null);
        assertThat(filtro.desde()).isEqualTo(t);
    }

    @Test
    void filtro_desdeMayorQueHasta_lanzaIllegalArgumentException() {
        OffsetDateTime desde = OffsetDateTime.now();
        OffsetDateTime hasta = desde.minusDays(1);
        assertThatThrownBy(() -> new FiltroEfectividad(desde, hasta, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desde");
    }

    @Test
    void empty_retornaHasDataFalse() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(30);
        OffsetDateTime hasta = OffsetDateTime.now();
        EfectividadModeloReporte reporte = EfectividadModeloReporte.empty(desde, hasta, null);
        assertThat(reporte.hasData()).isFalse();
        assertThat(reporte.overrides()).isEmpty();
        assertThat(reporte.matriz().celdas()).isEmpty();
    }

    @Test
    void empty_conAnalistaId_retornaAnalistaIdEnReporte() {
        EfectividadModeloReporte reporte = EfectividadModeloReporte.empty(
                OffsetDateTime.now().minusDays(30), OffsetDateTime.now(), "user_ana");
        assertThat(reporte.analistaId()).isEqualTo("user_ana");
        assertThat(reporte.hasData()).isFalse();
    }
}
