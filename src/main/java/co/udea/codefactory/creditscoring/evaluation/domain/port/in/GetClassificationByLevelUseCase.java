package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import java.time.OffsetDateTime;

import co.udea.codefactory.creditscoring.evaluation.domain.model.ClassificationItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

/**
 * Caso de uso para listar la última evaluación de cada solicitante
 * filtrada por nivel de riesgo y rango de fechas.
 */
public interface GetClassificationByLevelUseCase {

    /**
     * Retorna una página con la última evaluación por solicitante cuyo nivel coincide.
     *
     * @param nivel       nivel de riesgo a filtrar
     * @param desde       inicio del rango de fecha (puede ser nulo)
     * @param hasta       fin del rango de fecha (puede ser nulo)
     * @param pageRequest parámetros de paginación
     * @return resultado paginado de ítems de clasificación
     */
    PagedResult<ClassificationItem> porNivel(RiskLevel nivel, OffsetDateTime desde,
            OffsetDateTime hasta, PageRequest pageRequest);
}
