package co.udea.codefactory.creditscoring.reporting.domain.port.in;

import java.time.OffsetDateTime;

import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.reporting.domain.model.RiskDistributionReport;

/**
 * Puerto de entrada para obtener el reporte de distribución de riesgo.
 */
public interface GetRiskDistributionUseCase {

    /**
     * Genera el reporte de distribución de riesgo aplicando los filtros opcionales.
     *
     * @param desde      fecha inicio del rango (null = últimos 90 días)
     * @param hasta      fecha fin del rango (null = hoy)
     * @param tipoEmpleo tipo de empleo para filtrar (null = todos)
     * @return reporte con tabla por nivel, histograma y estadísticas globales
     */
    RiskDistributionReport report(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            EmploymentType tipoEmpleo);
}
