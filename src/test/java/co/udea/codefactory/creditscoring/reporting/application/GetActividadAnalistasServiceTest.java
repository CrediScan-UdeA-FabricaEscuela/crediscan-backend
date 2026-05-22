package co.udea.codefactory.creditscoring.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.reporting.application.service.analistas.GetActividadAnalistasService;
import co.udea.codefactory.creditscoring.reporting.application.util.BusinessHoursCalculator;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.ActividadAnalistasQueryPort;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.ActividadAnalistasQueryPort.AnalistaCountsAggregate;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.ActividadAnalistasQueryPort.AnalistaTimestampAggregate;

/**
 * Tests unitarios para GetActividadAnalistasService.
 * Usa Mockito para los ports de salida — sin Spring ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
class GetActividadAnalistasServiceTest {

    @Mock
    private ActividadAnalistasQueryPort queryPort;

    @Mock
    private BusinessHoursCalculator businessHoursCalculator;

    @InjectMocks
    private GetActividadAnalistasService service;

    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2025-03-31T23:59:59Z");

    // =========================================================================
    // Sin datos → hasData=false
    // =========================================================================

    @Test
    void sinDatos_retornaHasDataFalse() {
        when(queryPort.queryCounts(any(), any())).thenReturn(List.of());

        ActividadAnalistasReporte result = service.reporte(DESDE, HASTA);

        assertThat(result.hasData()).isFalse();
        assertThat(result.analistas()).isEmpty();
    }

    // =========================================================================
    // Tiempo medio calculado correctamente
    // =========================================================================

    @Test
    void tiempoMedio_calculadoConBusinessHours() {
        when(queryPort.queryCounts(any(), any())).thenReturn(List.of(
                new AnalistaCountsAggregate("user_ana", 2, 1, 1, 0, 0)
        ));

        OffsetDateTime t1 = OffsetDateTime.of(2025, 3, 3, 9, 0, 0, 0, ZoneOffset.ofHours(-5));
        OffsetDateTime t2 = OffsetDateTime.of(2025, 3, 3, 11, 0, 0, 0, ZoneOffset.ofHours(-5));
        OffsetDateTime t3 = OffsetDateTime.of(2025, 3, 3, 13, 0, 0, 0, ZoneOffset.ofHours(-5));
        OffsetDateTime t4 = OffsetDateTime.of(2025, 3, 3, 17, 0, 0, 0, ZoneOffset.ofHours(-5));

        when(queryPort.queryTimestamps(any(), any())).thenReturn(List.of(
                new AnalistaTimestampAggregate("user_ana", t1, t2),  // 2h
                new AnalistaTimestampAggregate("user_ana", t3, t4)   // 4h
        ));

        // businessHoursCalculator.calcular devuelve 2.0 y 4.0
        when(businessHoursCalculator.calcular(t1, t2)).thenReturn(2.0);
        when(businessHoursCalculator.calcular(t3, t4)).thenReturn(4.0);

        ActividadAnalistasReporte result = service.reporte(DESDE, HASTA);

        assertThat(result.hasData()).isTrue();
        assertThat(result.analistas()).hasSize(1);
        assertThat(result.analistas().get(0).tiempoMedioHorasHabiles()).isEqualTo(3.0); // (2+4)/2
    }

    // =========================================================================
    // N < 3 → outlierDetectionSkipped=true
    // =========================================================================

    @Test
    void menosDe3Analistas_outlierDetectionSkipped() {
        when(queryPort.queryCounts(any(), any())).thenReturn(List.of(
                new AnalistaCountsAggregate("user_ana", 5, 4, 1, 0, 0)
        ));
        when(queryPort.queryTimestamps(any(), any())).thenReturn(List.of());

        ActividadAnalistasReporte result = service.reporte(DESDE, HASTA);

        assertThat(result.estadisticasEquipo().outlierDetectionSkipped()).isTrue();
        assertThat(result.analistas().get(0).isOutlier()).isFalse();
    }

    // =========================================================================
    // Suma de porcentajes = 100 por analista
    // =========================================================================

    @Test
    void porcentajes_sumanA100() {
        when(queryPort.queryCounts(any(), any())).thenReturn(List.of(
                // 5 approved, 3 rejected, 1 manual, 1 escalated = 10 total
                new AnalistaCountsAggregate("user_ana", 10, 5, 3, 1, 1)
        ));
        when(queryPort.queryTimestamps(any(), any())).thenReturn(List.of());

        ActividadAnalistasReporte result = service.reporte(DESDE, HASTA);

        var dist = result.analistas().get(0).distribucion();
        var suma = dist.pctAprobacion()
                .add(dist.pctRechazo())
                .add(dist.pctManual())
                .add(dist.pctEscalado());

        assertThat(suma.doubleValue()).isEqualTo(100.0);
    }

    // =========================================================================
    // Estadísticas del equipo: numAnalistas y totalEvaluaciones
    // =========================================================================

    @Test
    void estadisticasEquipo_correctas() {
        when(queryPort.queryCounts(any(), any())).thenReturn(List.of(
                new AnalistaCountsAggregate("user_ana", 10, 8, 2, 0, 0),
                new AnalistaCountsAggregate("user_bob", 5, 3, 2, 0, 0)
        ));
        when(queryPort.queryTimestamps(any(), any())).thenReturn(List.of());

        ActividadAnalistasReporte result = service.reporte(DESDE, HASTA);

        assertThat(result.estadisticasEquipo().numAnalistas()).isEqualTo(2);
        assertThat(result.estadisticasEquipo().totalEvaluacionesEquipo()).isEqualTo(15L);
    }

    // =========================================================================
    // Rango invertido → IllegalArgumentException
    // =========================================================================

    @Test
    void rangoInvertido_lanzaExcepcion() {
        assertThatThrownBy(() -> service.reporte(HASTA, DESDE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // FIX-3: Filtro totalEvaluaciones >= 10 para detección de outliers (RN2-017)
    // =========================================================================

    /**
     * Un analista con menos de 10 evaluaciones NO debe ser marcado como outlier
     * ni incluirse en el cálculo de media/stddev del equipo, aunque su tiempo
     * sea extremadamente alto.
     */
    @Test
    void analistaConMenosDe10Evaluaciones_noEsOutlierNiInfluyeEnStddev() {
        // usuario_bajo tiene 5 evaluaciones (< 10) con tiempo absurdamente alto
        // usuario_a, usuario_b, usuario_c tienen >= 10 con tiempos normales
        when(queryPort.queryCounts(any(), any())).thenReturn(List.of(
                new AnalistaCountsAggregate("usuario_a",  10, 8, 2, 0, 0),
                new AnalistaCountsAggregate("usuario_b",  10, 7, 3, 0, 0),
                new AnalistaCountsAggregate("usuario_c",  10, 9, 1, 0, 0),
                new AnalistaCountsAggregate("usuario_bajo", 5, 4, 1, 0, 0)
        ));

        OffsetDateTime tStart       = OffsetDateTime.parse("2025-03-03T09:00:00Z");
        OffsetDateTime tNormal      = OffsetDateTime.parse("2025-03-03T11:00:00Z"); // 2h
        OffsetDateTime tAbsurdo     = OffsetDateTime.parse("2025-04-14T17:00:00Z"); // distinto timestamp

        when(queryPort.queryTimestamps(any(), any())).thenReturn(List.of(
                new AnalistaTimestampAggregate("usuario_a",   tStart, tNormal),
                new AnalistaTimestampAggregate("usuario_b",   tStart, tNormal),
                new AnalistaTimestampAggregate("usuario_c",   tStart, tNormal),
                new AnalistaTimestampAggregate("usuario_bajo", tStart, tAbsurdo)
        ));

        // Stubbing específico por instancia de timestamp
        when(businessHoursCalculator.calcular(eq(tStart), eq(tNormal))).thenReturn(2.0);
        when(businessHoursCalculator.calcular(eq(tStart), eq(tAbsurdo))).thenReturn(9999.0);

        ActividadAnalistasReporte result = service.reporte(DESDE, HASTA);

        // usuario_bajo NO debe ser marcado como outlier (tiene < 10 evaluaciones)
        var analistaBajo = result.analistas().stream()
                .filter(a -> a.evaluatedBy().equals("usuario_bajo"))
                .findFirst().orElseThrow();
        assertThat(analistaBajo.isOutlier())
                .as("analista con < 10 evaluaciones no debe ser outlier")
                .isFalse();

        // La detección no debe estar skipped (hay 3 analistas calificados)
        assertThat(result.estadisticasEquipo().outlierDetectionSkipped()).isFalse();
    }

    /**
     * Analistas con menos de 10 evaluaciones NO deben influir en el cálculo
     * de media/stddev: agregar dos analistas de bajo volumen no cambia el resultado
     * respecto al escenario sin ellos.
     */
    @Test
    void dosAnalistasBajoVolumen_noModificanMediaDelEquipo() {
        // Escenario base: 3 analistas calificados con tiempos [2.0, 2.0, 2.0]
        when(queryPort.queryCounts(any(), any())).thenReturn(List.of(
                new AnalistaCountsAggregate("u1",   12, 10, 2, 0, 0),
                new AnalistaCountsAggregate("u2",   11, 9,  2, 0, 0),
                new AnalistaCountsAggregate("u3",   10, 8,  2, 0, 0),
                new AnalistaCountsAggregate("bajo1",  3, 2,  1, 0, 0),
                new AnalistaCountsAggregate("bajo2",  7, 6,  1, 0, 0)
        ));

        OffsetDateTime t0 = OffsetDateTime.parse("2025-03-03T09:00:00Z");
        OffsetDateTime t1 = OffsetDateTime.parse("2025-03-03T11:00:00Z"); // 2h

        when(queryPort.queryTimestamps(any(), any())).thenReturn(List.of(
                new AnalistaTimestampAggregate("u1",    t0, t1),
                new AnalistaTimestampAggregate("u2",    t0, t1),
                new AnalistaTimestampAggregate("u3",    t0, t1),
                new AnalistaTimestampAggregate("bajo1", t0, t1),
                new AnalistaTimestampAggregate("bajo2", t0, t1)
        ));

        when(businessHoursCalculator.calcular(any(), any())).thenReturn(2.0);

        ActividadAnalistasReporte result = service.reporte(DESDE, HASTA);

        // Todos los calificados tienen el mismo tiempo → stddev=0, ningún outlier
        assertThat(result.estadisticasEquipo().outlierDetectionSkipped()).isFalse();
        result.analistas().stream()
                .filter(a -> List.of("u1", "u2", "u3").contains(a.evaluatedBy()))
                .forEach(a -> assertThat(a.isOutlier())
                        .as("analista calificado con tiempo igual a la media no es outlier: " + a.evaluatedBy())
                        .isFalse());
        result.analistas().stream()
                .filter(a -> List.of("bajo1", "bajo2").contains(a.evaluatedBy()))
                .forEach(a -> assertThat(a.isOutlier())
                        .as("analista de bajo volumen nunca es outlier: " + a.evaluatedBy())
                        .isFalse());
    }

    // =========================================================================
    // FIX-1: tasaAprobacionEquipo en EstadisticasEquipo (CA4-017)
    // =========================================================================

    /**
     * tasaAprobacionEquipo debe ser totalAprobaciones / totalDecisiones de los
     * analistas con totalEvaluaciones >= 10.
     */
    @Test
    void tasaAprobacionEquipo_calculadaSobreAnalistasCalificados() {
        // u1: 10 eval, 8 aprobadas, 2 rechazadas
        // u2: 10 eval, 4 aprobadas, 6 rechazadas
        // bajo: 5 eval (no califica) → excluir de tasaAprobacionEquipo
        when(queryPort.queryCounts(any(), any())).thenReturn(List.of(
                new AnalistaCountsAggregate("u1",   10, 8, 2, 0, 0),
                new AnalistaCountsAggregate("u2",   10, 4, 6, 0, 0),
                new AnalistaCountsAggregate("bajo",  5, 5, 0, 0, 0)
        ));
        when(queryPort.queryTimestamps(any(), any())).thenReturn(List.of());

        ActividadAnalistasReporte result = service.reporte(DESDE, HASTA);

        // totalAprobaciones calificados = 8 + 4 = 12
        // totalDecisiones calificados   = 10 + 10 = 20
        // tasaAprobacionEquipo = 12/20 = 60.00%
        var tasa = result.estadisticasEquipo().tasaAprobacionEquipo();
        assertThat(tasa).isNotNull();
        assertThat(tasa.doubleValue()).isEqualTo(60.0);
    }

    // =========================================================================
    // FIX-2: nombre en ActividadAnalista (CA1-017)
    // =========================================================================

    /**
     * nombre debe estar presente en cada analista con fallback a username
     * (evaluatedBy) cuando no hay fuente de nombre disponible.
     */
    @Test
    void analista_tieneNombreConFallbackAUsername() {
        when(queryPort.queryCounts(any(), any())).thenReturn(List.of(
                new AnalistaCountsAggregate("user_ana", 5, 4, 1, 0, 0)
        ));
        when(queryPort.queryTimestamps(any(), any())).thenReturn(List.of());

        ActividadAnalistasReporte result = service.reporte(DESDE, HASTA);

        var analista = result.analistas().get(0);
        // nombre debe existir y ser igual a evaluatedBy (fallback)
        assertThat(analista.nombre()).isNotNull().isNotEmpty();
        assertThat(analista.nombre()).isEqualTo(analista.evaluatedBy());
    }
}
