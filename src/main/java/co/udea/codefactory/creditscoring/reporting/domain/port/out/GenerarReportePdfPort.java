package co.udea.codefactory.creditscoring.reporting.domain.port.out;

import co.udea.codefactory.creditscoring.reporting.domain.model.RiskDistributionReport;

/**
 * Puerto de salida para generar el reporte en formato PDF.
 */
public interface GenerarReportePdfPort {

    /**
     * Genera el reporte en formato PDF y retorna los bytes del documento.
     *
     * @param report reporte a serializar
     * @return bytes del PDF generado
     */
    byte[] generar(RiskDistributionReport report);
}
