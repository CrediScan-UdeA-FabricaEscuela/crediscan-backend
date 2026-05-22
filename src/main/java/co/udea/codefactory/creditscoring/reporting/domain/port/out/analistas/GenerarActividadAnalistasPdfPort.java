package co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas;

import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;

/**
 * Puerto de salida para generar el reporte de actividad de analistas en PDF (HU-017).
 */
public interface GenerarActividadAnalistasPdfPort {

    /**
     * Genera el PDF del reporte de actividad de analistas.
     *
     * @param reporte reporte de dominio
     * @return bytes del PDF generado
     */
    byte[] generar(ActividadAnalistasReporte reporte);
}
