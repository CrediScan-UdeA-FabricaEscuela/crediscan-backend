package co.udea.codefactory.creditscoring.reporting.domain.model.analistas;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Reporte completo de actividad de analistas (HU-017).
 * <p>
 * Cuando {@code hasData=false}, los campos {@code analistas} y {@code estadisticasEquipo}
 * pueden estar vacíos. Nunca se retorna HTTP 404 — siempre HTTP 200.
 * </p>
 *
 * @param analistas          lista de métricas por analista
 * @param estadisticasEquipo estadísticas agregadas del equipo
 * @param hasData            true si existen evaluaciones con decisión en el rango
 * @param desde              inicio del rango consultado
 * @param hasta              fin del rango consultado
 */
public record ActividadAnalistasReporte(
        List<ActividadAnalista> analistas,
        EstadisticasEquipo estadisticasEquipo,
        boolean hasData,
        OffsetDateTime desde,
        OffsetDateTime hasta
) {
    /**
     * Crea un reporte vacío (hasData=false) para cuando no hay evaluaciones
     * en el rango solicitado.
     */
    public static ActividadAnalistasReporte empty(OffsetDateTime desde, OffsetDateTime hasta) {
        return new ActividadAnalistasReporte(
                List.of(),
                new EstadisticasEquipo(0L, 0.0, 0.0, 0, false, BigDecimal.ZERO.setScale(2)),
                false,
                desde,
                hasta);
    }
}
