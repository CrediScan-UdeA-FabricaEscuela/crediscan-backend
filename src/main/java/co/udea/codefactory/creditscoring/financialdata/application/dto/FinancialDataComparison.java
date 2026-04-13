package co.udea.codefactory.creditscoring.financialdata.application.dto;

import java.util.List;

import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;

/**
 * DTO de aplicación que representa el resultado de comparar dos versiones de
 * datos financieros de un mismo solicitante.
 *
 * <p>Contiene las dos versiones completas del dominio, la lista de campos que
 * cambiaron (con su estado de mejora o deterioro) y la tendencia general
 * calculada según RN1 de HU-005.</p>
 */
public record FinancialDataComparison(
        FinancialData base,
        FinancialData comparada,
        List<CampoComparado> camposModificados,
        String tendencia) {

    /**
     * Representa el cambio de un campo individual entre las dos versiones.
     * El estado indica si el cambio representa una mejora, deterioro o ausencia de cambio
     * según la semántica del campo (e.g., mayor deuda = deterioro, mayor activo = mejora).
     */
    public record CampoComparado(
            String campo,
            String valorBase,
            String valorComparado,
            String estado) {}
}
