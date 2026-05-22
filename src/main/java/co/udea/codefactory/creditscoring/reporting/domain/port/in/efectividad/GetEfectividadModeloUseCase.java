package co.udea.codefactory.creditscoring.reporting.domain.port.in.efectividad;

import java.time.OffsetDateTime;

import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.EfectividadModeloReporte;

/**
 * Puerto de entrada para el caso de uso de efectividad del modelo (HU-016).
 * <p>
 * Genera el reporte de la matriz de confusión, tasa de concordancia y
 * tasas de override para el rango de fechas indicado.
 * </p>
 */
public interface GetEfectividadModeloUseCase {

    /**
     * Genera el reporte de efectividad del modelo.
     *
     * @param desde      inicio del rango (obligatorio, inclusive)
     * @param hasta      fin del rango (obligatorio, inclusive)
     * @param analistaId filtro opcional por analista (null = todos)
     * @return reporte completo o {@code hasData=false} si no hay evaluaciones
     * @throws IllegalArgumentException si {@code desde} es posterior a {@code hasta}
     */
    EfectividadModeloReporte reporte(OffsetDateTime desde, OffsetDateTime hasta, String analistaId);
}
