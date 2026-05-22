package co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas;

import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;

/**
 * Puerto de salida para generar el reporte de actividad de analistas en CSV (HU-017).
 */
public interface GenerarActividadAnalistasCSVPort {

    /**
     * Genera el CSV del reporte de actividad de analistas.
     *
     * @param reporte reporte de dominio
     * @return bytes del CSV en UTF-8
     */
    byte[] generar(ActividadAnalistasReporte reporte);
}
