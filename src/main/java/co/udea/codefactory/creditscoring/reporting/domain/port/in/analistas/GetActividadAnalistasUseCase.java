package co.udea.codefactory.creditscoring.reporting.domain.port.in.analistas;

import java.time.OffsetDateTime;

import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;

/**
 * Puerto de entrada para el caso de uso de actividad de analistas (HU-017).
 */
public interface GetActividadAnalistasUseCase {

    /**
     * Genera el reporte de actividad de analistas.
     *
     * @param desde inicio del rango (obligatorio, inclusive)
     * @param hasta fin del rango (obligatorio, inclusive)
     * @return reporte completo o {@code hasData=false} si no hay evaluaciones
     * @throws IllegalArgumentException si {@code desde} es posterior a {@code hasta}
     */
    ActividadAnalistasReporte reporte(OffsetDateTime desde, OffsetDateTime hasta);
}
