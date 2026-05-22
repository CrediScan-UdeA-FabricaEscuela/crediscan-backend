package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalista;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.DistribucionDecisiones;
import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.EstadisticasEquipo;

/**
 * Tests unitarios del adaptador CSV de actividad de analistas.
 * Sin Spring — instancia directa.
 */
class ActividadAnalistasCSVAdapterTest {

    private final ActividadAnalistasCSVAdapter adapter = new ActividadAnalistasCSVAdapter();

    private static final OffsetDateTime DESDE = OffsetDateTime.now().minusDays(30);
    private static final OffsetDateTime HASTA = OffsetDateTime.now();

    // =========================================================================
    // Fila de encabezado presente
    // =========================================================================

    @Test
    void reporteSinDatos_soloEncabezado() {
        ActividadAnalistasReporte vacio = ActividadAnalistasReporte.empty(DESDE, HASTA);
        String csv = new String(adapter.generar(vacio), StandardCharsets.UTF_8);

        assertThat(csv).contains("analistaId");
        assertThat(csv).contains("totalEvaluaciones");
        assertThat(csv).contains("tasaAprobacion");
        // No debe haber filas de datos
        String[] lineas = csv.trim().split("\n");
        assertThat(lineas).hasSize(1); // solo la cabecera
    }

    // =========================================================================
    // Una fila por analista
    // =========================================================================

    @Test
    void reporteConDosAnalistas_dosFilasDeDatos() {
        ActividadAnalistasReporte reporte = buildReporteConAnalistas(
                "user_ana", "user_bob");
        String csv = new String(adapter.generar(reporte), StandardCharsets.UTF_8);

        String[] lineas = csv.trim().split("\n");
        // 1 cabecera + 2 analistas
        assertThat(lineas).hasSize(3);
    }

    // =========================================================================
    // Valores con comas correctamente entrecomillados
    // =========================================================================

    @Test
    void nombreConComa_correctamenteEntrecomillado() {
        DistribucionDecisiones dist = buildDist();
        EstadisticasEquipo equipo = new EstadisticasEquipo(5L, 2.0, 0.0, 1, true, BigDecimal.ZERO.setScale(2));
        // Analista con coma en el nombre (evaluatedBy y nombre iguales por fallback)
        ActividadAnalista analista = new ActividadAnalista("González, A.", "González, A.", 5, dist, 2.0, false);
        ActividadAnalistasReporte reporte = new ActividadAnalistasReporte(
                List.of(analista), equipo, true, DESDE, HASTA);

        String csv = new String(adapter.generar(reporte), StandardCharsets.UTF_8);
        // El valor con coma debe estar entre comillas dobles
        assertThat(csv).contains("\"González, A.\"");
    }

    // =========================================================================
    // Codificación UTF-8
    // =========================================================================

    @Test
    void csv_estaEnUtf8() {
        ActividadAnalistasReporte reporte = buildReporteConAnalistas("user_ana");
        byte[] bytes = adapter.generar(reporte);
        // Debe poder decodificarse como UTF-8 sin excepción
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).isNotEmpty();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ActividadAnalistasReporte buildReporteConAnalistas(String... ids) {
        EstadisticasEquipo equipo = new EstadisticasEquipo(
                (long) ids.length * 5, 2.0, 0.0, ids.length, true, BigDecimal.ZERO.setScale(2));
        List<ActividadAnalista> analistas = List.of(ids).stream()
                .map(id -> new ActividadAnalista(id, id, 5, buildDist(), 2.0, false))
                .toList();
        return new ActividadAnalistasReporte(analistas, equipo, true, DESDE, HASTA);
    }

    private DistribucionDecisiones buildDist() {
        return new DistribucionDecisiones(
                4, 1, 0, 0,
                new BigDecimal("80.00"), new BigDecimal("20.00"),
                BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2));
    }
}
