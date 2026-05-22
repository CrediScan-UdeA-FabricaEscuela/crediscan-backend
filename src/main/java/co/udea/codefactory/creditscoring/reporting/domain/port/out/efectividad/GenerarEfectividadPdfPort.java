package co.udea.codefactory.creditscoring.reporting.domain.port.out.efectividad;

import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.EfectividadModeloReporte;

/**
 * Puerto de salida para generar el reporte de efectividad del modelo en PDF (HU-016).
 */
public interface GenerarEfectividadPdfPort {

    /**
     * Genera el PDF del reporte de efectividad del modelo.
     *
     * @param reporte reporte de dominio
     * @return bytes del PDF generado
     */
    byte[] generar(EfectividadModeloReporte reporte);
}
