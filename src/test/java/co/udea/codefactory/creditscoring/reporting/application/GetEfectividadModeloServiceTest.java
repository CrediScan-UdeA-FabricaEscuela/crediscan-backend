package co.udea.codefactory.creditscoring.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.udea.codefactory.creditscoring.reporting.application.service.efectividad.GetEfectividadModeloService;
import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.EfectividadModeloReporte;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.efectividad.EfectividadModeloQueryPort;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.efectividad.EfectividadModeloQueryPort.MatrizAggregate;

/**
 * Tests unitarios para GetEfectividadModeloService.
 * Usa Mockito para el port de salida — sin Spring ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
class GetEfectividadModeloServiceTest {

    @Mock
    private EfectividadModeloQueryPort queryPort;

    @InjectMocks
    private GetEfectividadModeloService service;

    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2025-03-31T23:59:59Z");

    // =========================================================================
    // Sin datos → hasData=false
    // =========================================================================

    @Test
    void sinDatos_retornaHasDataFalse() {
        when(queryPort.queryMatriz(any(), any(), any())).thenReturn(List.of());

        EfectividadModeloReporte result = service.reporte(DESDE, HASTA, null);

        assertThat(result.hasData()).isFalse();
        assertThat(result.matriz().celdas()).isEmpty();
    }

    // =========================================================================
    // Gap-fill: siempre 20 celdas (5 niveles de riesgo × 4 decisiones)
    // =========================================================================

    @Test
    void conDatos_matrizContiene20Celdas() {
        // Solo 2 combinaciones devueltas por la query
        when(queryPort.queryMatriz(any(), any(), any())).thenReturn(List.of(
                new MatrizAggregate("VERY_LOW", "APPROVED", 5),
                new MatrizAggregate("HIGH", "REJECTED", 3)
        ));

        EfectividadModeloReporte result = service.reporte(DESDE, HASTA, null);

        assertThat(result.hasData()).isTrue();
        // 5 niveles (VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH) × 4 decisiones = 20 celdas
        assertThat(result.matriz().celdas()).hasSize(20);
    }

    @Test
    void celdaSinDatos_tieneCount0() {
        when(queryPort.queryMatriz(any(), any(), any())).thenReturn(List.of(
                new MatrizAggregate("VERY_LOW", "APPROVED", 10)
        ));

        EfectividadModeloReporte result = service.reporte(DESDE, HASTA, null);

        // Celda LOW+REJECTED debe existir con count=0
        boolean cellaLowRejectedPresent = result.matriz().celdas().stream()
                .anyMatch(c -> c.riskLevel().name().equals("LOW")
                        && c.decision().name().equals("REJECTED")
                        && c.count() == 0);
        assertThat(cellaLowRejectedPresent).isTrue();
    }

    // =========================================================================
    // REJECTED risk_level agrupado bajo VERY_HIGH
    // =========================================================================

    @Test
    void riskLevelRejected_seAgrupaEnVeryHigh() {
        when(queryPort.queryMatriz(any(), any(), any())).thenReturn(List.of(
                new MatrizAggregate("REJECTED", "REJECTED", 7),
                new MatrizAggregate("VERY_HIGH", "REJECTED", 3)
        ));

        EfectividadModeloReporte result = service.reporte(DESDE, HASTA, null);

        // VERY_HIGH + REJECTED debe tener 7+3=10
        long veryHighRejected = result.matriz().celdas().stream()
                .filter(c -> c.riskLevel().name().equals("VERY_HIGH")
                        && c.decision().name().equals("REJECTED"))
                .mapToLong(c -> c.count())
                .sum();
        assertThat(veryHighRejected).isEqualTo(10L);

        // REJECTED no debe aparecer como fila en la matriz
        boolean rejectedRowPresent = result.matriz().celdas().stream()
                .anyMatch(c -> c.riskLevel().name().equals("REJECTED"));
        assertThat(rejectedRowPresent).isFalse();
    }

    // =========================================================================
    // Tasa de concordancia = 80.00
    // =========================================================================

    @Test
    void concordanceRate_80de100_retorna80punto00() {
        // 80 VERY_LOW+APPROVED (concordantes) + 20 LOW+REJECTED (overrides)
        // Denominador = APPROVED + REJECTED = 100
        // concordanceRate = 80/100 = 80.00
        when(queryPort.queryMatriz(any(), any(), any())).thenReturn(List.of(
                new MatrizAggregate("VERY_LOW", "APPROVED", 80),
                new MatrizAggregate("LOW", "REJECTED", 20)
        ));

        EfectividadModeloReporte result = service.reporte(DESDE, HASTA, null);

        assertThat(result.indicadores().tasaConcordanciaGlobal())
                .isEqualByComparingTo(new BigDecimal("80.00"));
    }

    // =========================================================================
    // Tasas de override
    // =========================================================================

    @Test
    void overrideRates_calculadasCorrectamente() {
        // 10 HIGH+APPROVED (override aprobación), 5 VERY_LOW+REJECTED (override rechazo)
        // 85 VERY_LOW+APPROVED (concordantes)
        // Total activo = 10+5+85 = 100
        // overrideApprovalRate = 10/100 = 10.00
        // overrideRejectionRate = 5/100 = 5.00
        when(queryPort.queryMatriz(any(), any(), any())).thenReturn(List.of(
                new MatrizAggregate("HIGH", "APPROVED", 10),
                new MatrizAggregate("VERY_LOW", "REJECTED", 5),
                new MatrizAggregate("VERY_LOW", "APPROVED", 85)
        ));

        EfectividadModeloReporte result = service.reporte(DESDE, HASTA, null);

        assertThat(result.indicadores().tasaOverrideAprobacion())
                .isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(result.indicadores().tasaOverrideRechazo())
                .isEqualByComparingTo(new BigDecimal("5.00"));
    }

    // =========================================================================
    // Filtro por analistaId — se pasa al port
    // =========================================================================

    @Test
    void filtroAnalistaId_seEnviaAlPort() {
        when(queryPort.queryMatriz(any(), any(), eq("user_ana"))).thenReturn(List.of(
                new MatrizAggregate("LOW", "APPROVED", 3)
        ));

        EfectividadModeloReporte result = service.reporte(DESDE, HASTA, "user_ana");

        assertThat(result.analistaId()).isEqualTo("user_ana");
        assertThat(result.hasData()).isTrue();
    }

    // =========================================================================
    // analistaId sin resultados → hasData=false (no 400)
    // =========================================================================

    @Test
    void filtroAnalistaId_sinResultados_retornaHasDataFalse() {
        when(queryPort.queryMatriz(any(), any(), eq("user_inexistente"))).thenReturn(List.of());

        EfectividadModeloReporte result = service.reporte(DESDE, HASTA, "user_inexistente");

        assertThat(result.hasData()).isFalse();
    }

    // =========================================================================
    // Rango invertido → IllegalArgumentException
    // =========================================================================

    @Test
    void rangoInvertido_lanzaExcepcion() {
        assertThatThrownBy(() -> service.reporte(HASTA, DESDE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
