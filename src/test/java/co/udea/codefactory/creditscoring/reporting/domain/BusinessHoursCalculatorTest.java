package co.udea.codefactory.creditscoring.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.reporting.application.util.BusinessHoursCalculator;

/**
 * Tests unitarios para BusinessHoursCalculator.
 * Ventana hábil: Lunes-Viernes 08:00 (inclusive) - 18:00 (exclusive).
 * Sin Spring — JUnit 5 puro.
 */
class BusinessHoursCalculatorTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /** Crea un OffsetDateTime a las horas indicadas en zona Bogotá (lunes 2025-03-03). */
    private static OffsetDateTime lunes(int hora, int minuto) {
        return OffsetDateTime.of(2025, 3, 3, hora, minuto, 0, 0, ZoneOffset.ofHours(-5));
    }

    private static OffsetDateTime viernes(int hora, int minuto) {
        // 2025-03-07 es viernes
        return OffsetDateTime.of(2025, 3, 7, hora, minuto, 0, 0, ZoneOffset.ofHours(-5));
    }

    private static OffsetDateTime lunesProximo(int hora, int minuto) {
        // 2025-03-10 es lunes siguiente al viernes 07
        return OffsetDateTime.of(2025, 3, 10, hora, minuto, 0, 0, ZoneOffset.ofHours(-5));
    }

    // =========================================================================
    // Caso 1: start == end → 0.0
    // =========================================================================

    @Test
    void startIgualEnd_retornaCero() {
        OffsetDateTime t = lunes(9, 0);
        assertThat(BusinessHoursCalculator.between(t, t, BOGOTA)).isEqualTo(0.0);
    }

    // =========================================================================
    // Caso 2: lunes 09:00 → 11:00 = 2.0 horas hábiles
    // =========================================================================

    @Test
    void lunes9a11_retorna2horas() {
        assertThat(BusinessHoursCalculator.between(lunes(9, 0), lunes(11, 0), BOGOTA))
                .isEqualTo(2.0);
    }

    // =========================================================================
    // Caso 3: lunes 17:00 → 19:00 = 1.0 hora (solo cuenta hasta las 18:00)
    // =========================================================================

    @Test
    void lunes17a19_retorna1hora() {
        assertThat(BusinessHoursCalculator.between(lunes(17, 0), lunes(19, 0), BOGOTA))
                .isEqualTo(1.0);
    }

    // =========================================================================
    // Caso 4: viernes 17:00 → lunes siguiente 09:00 = 2.0 horas
    //         (viernes 17-18 = 1h, lunes 08-09 = 1h, fin de semana = 0)
    // =========================================================================

    @Test
    void viernesALunesProximo_retorna2horas() {
        assertThat(BusinessHoursCalculator.between(viernes(17, 0), lunesProximo(9, 0), BOGOTA))
                .isEqualTo(2.0);
    }

    // =========================================================================
    // Caso 5: lunes 16:00 → 18:00 = 2.0 horas (18:00 es el límite, inclusive)
    // =========================================================================

    @Test
    void lunes16a18_retorna2horas() {
        assertThat(BusinessHoursCalculator.between(lunes(16, 0), lunes(18, 0), BOGOTA))
                .isEqualTo(2.0);
    }

    // =========================================================================
    // Caso 6: lunes 20:00 → 20:30 = 0.0 horas (fuera de horario hábil)
    // =========================================================================

    @Test
    void lunes20a2030_retornaCero() {
        assertThat(BusinessHoursCalculator.between(lunes(20, 0), lunes(20, 30), BOGOTA))
                .isEqualTo(0.0);
    }

    // =========================================================================
    // Caso 7: ambas en fin de semana = 0.0
    // =========================================================================

    @Test
    void finDeSemana_retornaCero() {
        // 2025-03-08 sábado
        OffsetDateTime sabado = OffsetDateTime.of(2025, 3, 8, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
        OffsetDateTime sabadoFin = OffsetDateTime.of(2025, 3, 8, 12, 0, 0, 0, ZoneOffset.ofHours(-5));
        assertThat(BusinessHoursCalculator.between(sabado, sabadoFin, BOGOTA))
                .isEqualTo(0.0);
    }

    // =========================================================================
    // Caso 8: start > end → lanza IllegalArgumentException
    // =========================================================================

    @Test
    void startMayorQueEnd_lanzaExcepcion() {
        assertThatThrownBy(() ->
                BusinessHoursCalculator.between(lunes(11, 0), lunes(9, 0), BOGOTA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // Caso 9: span de múltiples días (lunes 08:00 → miércoles 10:00)
    //         lunes: 10h, martes: 10h, miércoles: 2h = 22h
    // =========================================================================

    @Test
    void multiDia_lunesAMiercoles_retorna22horas() {
        OffsetDateTime start = lunes(8, 0);
        // miércoles 2025-03-05 10:00
        OffsetDateTime end = OffsetDateTime.of(2025, 3, 5, 10, 0, 0, 0, ZoneOffset.ofHours(-5));
        assertThat(BusinessHoursCalculator.between(start, end, BOGOTA))
                .isEqualTo(22.0);
    }

    // =========================================================================
    // Caso 10: lunes 08:00 → 08:00 exacto = 0.0 (ya cubierto por startIgualEnd)
    // =========================================================================

    @Test
    void lunes8a8_retornaCero() {
        OffsetDateTime t = lunes(8, 0);
        assertThat(BusinessHoursCalculator.between(t, t, BOGOTA)).isEqualTo(0.0);
    }

    // =========================================================================
    // Caso 11: inicio antes del horario hábil — 06:00 → 09:00 = 1.0 hora
    // =========================================================================

    @Test
    void inicioAntesHorario_cuentaDesde8() {
        assertThat(BusinessHoursCalculator.between(lunes(6, 0), lunes(9, 0), BOGOTA))
                .isEqualTo(1.0);
    }

    // =========================================================================
    // Caso 12: fin exactamente a las 18:00 — solo cuenta hasta 18:00 (exclusive→ 2h)
    // El intervalo [16:00,18:00) tiene 2h, y [16:00,18:00] también tiene 2h
    // porque efectiveEnd = min(18:00, end) = 18:00 y duration = 18:00 - 16:00 = 2h
    // =========================================================================

    @Test
    void finExactamente18_retorna2horas() {
        assertThat(BusinessHoursCalculator.between(lunes(16, 0), lunes(18, 0), BOGOTA))
                .isEqualTo(2.0);
    }

    // =========================================================================
    // Caso 13: lunes 08:00 → lunes 18:00 = 10 horas exactas (día completo)
    // =========================================================================

    @Test
    void diaCompletoHabiles_retorna10horas() {
        assertThat(BusinessHoursCalculator.between(lunes(8, 0), lunes(18, 0), BOGOTA))
                .isEqualTo(10.0);
    }
}
