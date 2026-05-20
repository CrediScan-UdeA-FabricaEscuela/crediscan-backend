package co.udea.codefactory.creditscoring.reporting.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Reporte de distribución de riesgo completo.
 * Contiene la tabla por nivel, el histograma de 10 bins, estadísticas globales
 * y metadatos del filtro aplicado.
 */
public record RiskDistributionReport(
        List<RiskLevelSummary> tabla,
        List<HistogramBin> histograma,
        OverallStats overall,
        boolean hasData,
        OffsetDateTime fechaDesde,
        OffsetDateTime fechaHasta,
        String tipoEmpleo
) {
    /**
     * Crea un reporte vacío (hasData=false) para cuando no hay evaluaciones
     * en el rango solicitado. Nunca retorna 404.
     */
    public static RiskDistributionReport empty(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            String tipoEmpleo) {
        return new RiskDistributionReport(
                List.of(),
                HistogramBin.empty10Bins(),
                OverallStats.zero(),
                false,
                desde,
                hasta,
                tipoEmpleo);
    }
}
