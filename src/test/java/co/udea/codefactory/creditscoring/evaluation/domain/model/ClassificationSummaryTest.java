package co.udea.codefactory.creditscoring.evaluation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios para ClassificationSummary.
 * Verifica que la factory siempre retorna los 6 niveles de riesgo,
 * rellenando con 0 los ausentes.
 */
class ClassificationSummaryTest {

    @Test
    void crear_mapVacio_retornaSeisnivelesConCerosTodos() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(90);
        OffsetDateTime hasta = OffsetDateTime.now();

        ClassificationSummary summary = ClassificationSummary.crear(Map.of(), desde, hasta);

        assertThat(summary.levels()).hasSize(6);
        assertThat(summary.levels()).allMatch(lc -> lc.count() == 0);
        assertThat(summary.desde()).isEqualTo(desde);
        assertThat(summary.hasta()).isEqualTo(hasta);
    }

    @Test
    void crear_mapConAlgunosNiveles_rellenaCerosParaAusentes() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(30);
        OffsetDateTime hasta = OffsetDateTime.now();

        Map<RiskLevel, Long> counts = Map.of(
                RiskLevel.LOW, 5L,
                RiskLevel.HIGH, 3L);

        ClassificationSummary summary = ClassificationSummary.crear(counts, desde, hasta);

        assertThat(summary.levels()).hasSize(6);

        Map<RiskLevel, Long> byLevel = summary.levels().stream()
                .collect(Collectors.toMap(LevelCount::level, LevelCount::count));

        assertThat(byLevel.get(RiskLevel.LOW)).isEqualTo(5L);
        assertThat(byLevel.get(RiskLevel.HIGH)).isEqualTo(3L);
        assertThat(byLevel.get(RiskLevel.VERY_LOW)).isEqualTo(0L);
        assertThat(byLevel.get(RiskLevel.MEDIUM)).isEqualTo(0L);
        assertThat(byLevel.get(RiskLevel.VERY_HIGH)).isEqualTo(0L);
        assertThat(byLevel.get(RiskLevel.REJECTED)).isEqualTo(0L);
    }

    @Test
    void crear_mapConTodosLosNiveles_retornaCountsCorrectos() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(7);
        OffsetDateTime hasta = OffsetDateTime.now();

        Map<RiskLevel, Long> counts = Map.of(
                RiskLevel.VERY_LOW, 10L,
                RiskLevel.LOW, 8L,
                RiskLevel.MEDIUM, 6L,
                RiskLevel.HIGH, 4L,
                RiskLevel.VERY_HIGH, 2L,
                RiskLevel.REJECTED, 1L);

        ClassificationSummary summary = ClassificationSummary.crear(counts, desde, hasta);

        assertThat(summary.levels()).hasSize(6);
        long total = summary.levels().stream().mapToLong(LevelCount::count).sum();
        assertThat(total).isEqualTo(31L);
    }
}
