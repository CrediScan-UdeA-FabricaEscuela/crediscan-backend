package co.udea.codefactory.creditscoring.reporting.domain.model.analistas;

import java.time.OffsetDateTime;

/**
 * Filtro de búsqueda para el reporte de actividad de analistas (HU-017).
 * <p>
 * Ambas fechas son obligatorias. Se valida que {@code desde} no sea posterior a {@code hasta}.
 * </p>
 *
 * @param desde inicio del rango (inclusive)
 * @param hasta fin del rango (inclusive)
 */
public record FiltroActividad(OffsetDateTime desde, OffsetDateTime hasta) {
    /**
     * Valida que el rango sea coherente.
     *
     * @throws IllegalArgumentException si {@code desde} es posterior a {@code hasta}
     */
    public FiltroActividad {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "El parámetro 'desde' (" + desde + ") no puede ser posterior a 'hasta' (" + hasta + ")");
        }
    }
}
