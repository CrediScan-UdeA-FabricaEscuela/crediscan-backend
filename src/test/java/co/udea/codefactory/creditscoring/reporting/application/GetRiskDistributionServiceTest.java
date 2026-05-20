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

import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.reporting.application.service.GetRiskDistributionService;
import co.udea.codefactory.creditscoring.reporting.domain.model.RiskDistributionReport;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.RiskDistributionQueryPort;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.RiskDistributionQueryPort.BinAggregate;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.RiskDistributionQueryPort.LevelAggregate;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.RiskDistributionQueryPort.OverallAggregate;

/**
 * Tests unitarios del service de distribución de riesgo.
 * Verifica: no-data, gap-fill, score=100 clamp, default 90 días, stddev null, rango invertido.
 */
@ExtendWith(MockitoExtension.class)
class GetRiskDistributionServiceTest {

    @Mock
    private RiskDistributionQueryPort queryPort;

    @InjectMocks
    private GetRiskDistributionService service;

    // =========================================================================
    // Sin datos → hasData=false
    // =========================================================================

    @Test
    void sinDatos_retornaHasDataFalse() {
        when(queryPort.overallStats(any(), any(), any()))
                .thenReturn(new OverallAggregate(0L, BigDecimal.ZERO, BigDecimal.ZERO));

        RiskDistributionReport result = service.report(
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now(),
                null);

        assertThat(result.hasData()).isFalse();
        assertThat(result.tabla()).isEmpty();
        assertThat(result.histograma()).hasSize(10);
    }

    // =========================================================================
    // Gap-fill: siempre 10 bins
    // =========================================================================

    @Test
    void conDatos_histogramasiempre10Bins() {
        var desde = OffsetDateTime.now().minusDays(10);
        var hasta = OffsetDateTime.now();

        when(queryPort.overallStats(any(), any(), any()))
                .thenReturn(new OverallAggregate(2L, BigDecimal.valueOf(40), BigDecimal.ZERO));
        when(queryPort.distributionByLevel(any(), any(), any()))
                .thenReturn(List.of(
                        new LevelAggregate(RiskLevel.HIGH, 2L, BigDecimal.valueOf(40))));
        // Solo se retorna bin 30-40 desde la DB
        when(queryPort.histogram(any(), any(), any()))
                .thenReturn(List.of(new BinAggregate(30, 2L)));

        RiskDistributionReport result = service.report(desde, hasta, null);

        assertThat(result.hasData()).isTrue();
        assertThat(result.histograma()).hasSize(10);
        // Los bins sin datos deben tener count=0
        assertThat(result.histograma().stream()
                .filter(b -> b.binStart() == 0).findFirst().orElseThrow().count()).isEqualTo(0L);
        assertThat(result.histograma().stream()
                .filter(b -> b.binStart() == 30).findFirst().orElseThrow().count()).isEqualTo(2L);
    }

    // =========================================================================
    // Fechas null → defecto 90 días
    // =========================================================================

    @Test
    void fechasNull_usaDefecto90Dias() {
        when(queryPort.overallStats(any(), any(), any()))
                .thenReturn(new OverallAggregate(0L, BigDecimal.ZERO, BigDecimal.ZERO));

        // No debe lanzar excepción
        RiskDistributionReport result = service.report(null, null, null);
        assertThat(result).isNotNull();
        assertThat(result.fechaDesde()).isNotNull();
        assertThat(result.fechaHasta()).isNotNull();
        // La diferencia debe ser ~90 días
        assertThat(result.fechaHasta().toEpochSecond() - result.fechaDesde().toEpochSecond())
                .isGreaterThanOrEqualTo(89 * 86400L);
    }

    // =========================================================================
    // stddev null → ZERO
    // =========================================================================

    @Test
    void stddevNull_seConvierteACero() {
        var desde = OffsetDateTime.now().minusDays(10);
        var hasta = OffsetDateTime.now();

        when(queryPort.overallStats(any(), any(), any()))
                .thenReturn(new OverallAggregate(1L, BigDecimal.valueOf(75), null));
        when(queryPort.distributionByLevel(any(), any(), any()))
                .thenReturn(List.of(
                        new LevelAggregate(RiskLevel.LOW, 1L, BigDecimal.valueOf(75))));
        when(queryPort.histogram(any(), any(), any()))
                .thenReturn(List.of(new BinAggregate(70, 1L)));

        RiskDistributionReport result = service.report(desde, hasta, null);

        assertThat(result.overall().stdDev()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // avg null → ZERO
    // =========================================================================

    @Test
    void avgNull_seConvierteACero() {
        var desde = OffsetDateTime.now().minusDays(10);
        var hasta = OffsetDateTime.now();

        when(queryPort.overallStats(any(), any(), any()))
                .thenReturn(new OverallAggregate(1L, null, BigDecimal.ZERO));
        when(queryPort.distributionByLevel(any(), any(), any()))
                .thenReturn(List.of(
                        new LevelAggregate(RiskLevel.LOW, 1L, BigDecimal.valueOf(75))));
        when(queryPort.histogram(any(), any(), any()))
                .thenReturn(List.of(new BinAggregate(70, 1L)));

        RiskDistributionReport result = service.report(desde, hasta, null);

        assertThat(result.overall().averageScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // Rango invertido → IllegalArgumentException
    // =========================================================================

    @Test
    void rangoInvertido_lanzaIllegalArgumentException() {
        var desde = OffsetDateTime.now();
        var hasta = desde.minusDays(1);

        assertThatThrownBy(() -> service.report(desde, hasta, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha_desde");
    }

    // =========================================================================
    // Filtro por tipoEmpleo
    // =========================================================================

    @Test
    void conTipoEmpleo_pasaApiValueAlPort() {
        var desde = OffsetDateTime.now().minusDays(10);
        var hasta = OffsetDateTime.now();

        when(queryPort.overallStats(any(), any(), eq("Empleado")))
                .thenReturn(new OverallAggregate(0L, BigDecimal.ZERO, BigDecimal.ZERO));

        RiskDistributionReport result = service.report(desde, hasta, EmploymentType.EMPLEADO);
        assertThat(result.tipoEmpleo()).isEqualTo("Empleado");
    }
}
