package co.udea.codefactory.creditscoring.reporting.domain.model.efectividad;

import java.time.OffsetDateTime;

/**
 * Filtro de búsqueda para el reporte de efectividad del modelo.
 * <p>
 * Ambas fechas son obligatorias. Si {@code analistaId} es null, se incluyen
 * evaluaciones de todos los analistas.
 * </p>
 *
 * @param desde     inicio del rango (inclusive)
 * @param hasta     fin del rango (inclusive)
 * @param analistaId identificador del analista para filtrar (null = todos)
 */
public record FiltroEfectividad(
        OffsetDateTime desde,
        OffsetDateTime hasta,
        String analistaId
) {
    /**
     * Valida que el rango sea coherente.
     *
     * @throws IllegalArgumentException si {@code desde} es posterior a {@code hasta}
     */
    public FiltroEfectividad {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "El parámetro 'desde' (" + desde + ") no puede ser posterior a 'hasta' (" + hasta + ")");
        }
    }
}
