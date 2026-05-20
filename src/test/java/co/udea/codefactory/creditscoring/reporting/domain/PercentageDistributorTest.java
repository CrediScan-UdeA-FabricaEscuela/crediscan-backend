package co.udea.codefactory.creditscoring.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.reporting.application.util.PercentageDistributor;

/**
 * Tests unitarios del distribuidor de porcentajes.
 * Verifica que la suma sea exactamente 100.00 y el tie-break sea determinístico.
 */
class PercentageDistributorTest {

    @Test
    void tresPorcionesIguales_sumaExactamente100() {
        List<Long> counts = List.of(1L, 1L, 1L);
        List<BigDecimal> result = PercentageDistributor.largestRemainder(counts, 3L, 2);

        BigDecimal sum = result.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result).allMatch(p -> p.scale() == 2);
    }

    @Test
    void tresPorcionesIguales_tieBreakPorIndiceAscendente() {
        // 1/3 cada una: 33.33, 33.33, 33.34 — el índice 0 recibe el residuo extra
        List<Long> counts = List.of(1L, 1L, 1L);
        List<BigDecimal> result = PercentageDistributor.largestRemainder(counts, 3L, 2);

        // Los tres tienen el mismo residuo; tie-break índice ascendente → índice 0 lleva el +0.01
        // 33.34, 33.33, 33.33
        assertThat(result.get(0)).isEqualByComparingTo("33.34");
        assertThat(result.get(1)).isEqualByComparingTo("33.33");
        assertThat(result.get(2)).isEqualByComparingTo("33.33");
    }

    @Test
    void elementoUnico_es100() {
        List<Long> counts = List.of(7L);
        List<BigDecimal> result = PercentageDistributor.largestRemainder(counts, 7L, 2);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualByComparingTo("100.00");
    }

    @Test
    void totalCero_retornaTodosCero() {
        List<Long> counts = List.of(0L, 0L, 0L);
        List<BigDecimal> result = PercentageDistributor.largestRemainder(counts, 0L, 2);
        assertThat(result).allMatch(p -> p.compareTo(BigDecimal.ZERO) == 0);
        assertThat(result).allMatch(p -> p.scale() == 2);
    }

    @Test
    void distribucionDesglosada_sumaExactamente100() {
        // 10 elementos con conteos distintos
        List<Long> counts = List.of(4L, 3L, 2L, 1L);
        List<BigDecimal> result = PercentageDistributor.largestRemainder(counts, 10L, 2);
        BigDecimal sum = result.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void tieBreakDeterministico_mismoOrdenSiempreProduceMismoResultado() {
        List<Long> counts = List.of(1L, 1L, 1L);
        List<BigDecimal> first = PercentageDistributor.largestRemainder(counts, 3L, 2);
        List<BigDecimal> second = PercentageDistributor.largestRemainder(counts, 3L, 2);
        assertThat(first).isEqualTo(second);
    }
}
