package co.udea.codefactory.creditscoring.financialdata.domain.port.in;

import java.util.List;
import java.util.UUID;

import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataComparison;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;

/**
 * Puerto de entrada para las operaciones de consulta de historial financiero (HU-005).
 */
public interface GetFinancialHistoryUseCase {

    /**
     * Retorna el historial completo de datos financieros de un solicitante,
     * ordenado por versión descendente (más reciente primero).
     *
     * @param solicitanteId identificador del solicitante
     * @return lista de versiones ordenadas (puede ser vacía si no hay datos aún)
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException si el solicitante no existe
     */
    List<FinancialData> obtenerHistorial(UUID solicitanteId);

    /**
     * Compara dos versiones de datos financieros del mismo solicitante.
     * La versión con número menor se toma como base; la de mayor número como la comparada.
     *
     * @param solicitanteId identificador del solicitante
     * @param version1 número de la primera versión a comparar
     * @param version2 número de la segunda versión a comparar (debe ser distinto de version1)
     * @return comparación con campos modificados y tendencia general
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException si el solicitante o alguna versión no existe
     * @throws co.udea.codefactory.creditscoring.financialdata.domain.exception.InvalidFinancialDataException si version1 == version2
     */
    FinancialDataComparison comparar(UUID solicitanteId, int version1, int version2);
}
