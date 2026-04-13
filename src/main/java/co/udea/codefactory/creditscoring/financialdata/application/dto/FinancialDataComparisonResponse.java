package co.udea.codefactory.creditscoring.financialdata.application.dto;

import java.util.List;

/**
 * DTO de respuesta REST para la comparación entre dos versiones de datos financieros.
 * Incluye los datos completos de cada versión, los campos que cambiaron (con estado
 * de mejora/deterioro) y la tendencia general calculada en el backend.
 */
public record FinancialDataComparisonResponse(
        FinancialDataResponse versionBase,
        FinancialDataResponse versionComparada,
        List<CampoComparado> camposModificados,
        String tendencia) {

    /** Detalle de un campo individual que cambió entre las dos versiones. */
    public record CampoComparado(
            String campo,
            String valorBase,
            String valorComparado,
            String estado) {}
}
