package co.udea.codefactory.creditscoring.reporting.application.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Calculadora de horas hábiles entre dos instantes de tiempo.
 * <p>
 * Ventana hábil: Lunes–Viernes 08:00 (inclusive) – 18:00 (exclusive),
 * en la zona horaria configurada por {@code app.reporting.business-timezone}
 * (valor por defecto: {@code America/Bogota}).
 * </p>
 * <p>
 * El método estático {@link #between(OffsetDateTime, OffsetDateTime, ZoneId)}
 * permite reutilización en tests sin necesidad de Spring.
 * </p>
 */
@Component
public class BusinessHoursCalculator {

    private static final LocalTime WINDOW_OPEN  = LocalTime.of(8, 0);
    private static final LocalTime WINDOW_CLOSE = LocalTime.of(18, 0);

    private final ZoneId zoneId;

    public BusinessHoursCalculator(
            @Value("${app.reporting.business-timezone:America/Bogota}") String timezone) {
        this.zoneId = ZoneId.of(timezone);
    }

    /**
     * Calcula las horas hábiles entre {@code start} y {@code end} usando la zona horaria
     * configurada en la instancia.
     *
     * @param start instante de inicio
     * @param end   instante de fin
     * @return horas hábiles (puede ser fraccionario)
     */
    public double calcular(OffsetDateTime start, OffsetDateTime end) {
        return between(start, end, zoneId);
    }

    /**
     * Calcula las horas hábiles entre {@code start} y {@code end} en la zona {@code zoneId}.
     * <p>Método estático para facilitar el test sin contexto de Spring.</p>
     *
     * @param start  instante de inicio
     * @param end    instante de fin
     * @param zoneId zona horaria para la ventana de trabajo
     * @return horas hábiles (puede ser fraccionario)
     * @throws IllegalArgumentException si {@code end} es anterior a {@code start}
     */
    public static double between(OffsetDateTime start, OffsetDateTime end, ZoneId zoneId) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "El instante de fin no puede ser anterior al de inicio");
        }
        if (start.isEqual(end)) {
            return 0.0;
        }

        ZonedDateTime zStart = start.atZoneSameInstant(zoneId);
        ZonedDateTime zEnd   = end.atZoneSameInstant(zoneId);

        LocalDate diaActual = zStart.toLocalDate();
        LocalDate diaFin    = zEnd.toLocalDate();

        long totalMinutes = 0;

        while (!diaActual.isAfter(diaFin)) {
            totalMinutes += businessMinutesForDay(diaActual, zStart, zEnd);
            diaActual = diaActual.plusDays(1);
        }

        return totalMinutes / 60.0;
    }

    /**
     * Calcula los minutos hábiles que caen dentro del día {@code dia} y dentro del
     * intervalo [zStart, zEnd).
     */
    private static long businessMinutesForDay(LocalDate dia, ZonedDateTime zStart, ZonedDateTime zEnd) {
        // Solo días de semana (Lunes - Viernes)
        DayOfWeek dow = dia.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return 0;
        }

        ZoneId zone = zStart.getZone();

        ZonedDateTime ventanaApertura = dia.atTime(WINDOW_OPEN).atZone(zone);
        ZonedDateTime ventanaCierre   = dia.atTime(WINDOW_CLOSE).atZone(zone);

        // Rango efectivo = intersección de [zStart, zEnd] con [ventanaApertura, ventanaCierre]
        ZonedDateTime efectivoInicio = max(ventanaApertura, zStart);
        ZonedDateTime efectivoFin    = min(ventanaCierre, zEnd);

        if (!efectivoFin.isAfter(efectivoInicio)) {
            return 0;
        }

        return Duration.between(efectivoInicio, efectivoFin).toMinutes();
    }

    private static ZonedDateTime max(ZonedDateTime a, ZonedDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static ZonedDateTime min(ZonedDateTime a, ZonedDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
