package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import java.time.OffsetDateTime;

import co.udea.codefactory.creditscoring.evaluation.domain.model.ClassificationSummary;

/**
 * Caso de uso para obtener el resumen de clasificación de riesgo del portafolio.
 * Siempre retorna los 6 niveles de riesgo aunque el conteo sea 0.
 */
public interface GetEvaluationClassificationUseCase {

    /**
     * Retorna el resumen de clasificación para el rango de fechas dado.
     * Si {@code desde} o {@code hasta} son nulos, se aplica el rango predeterminado.
     *
     * @param desde inicio del rango (puede ser nulo)
     * @param hasta fin del rango (puede ser nulo)
     * @return resumen con exactamente 6 entradas de nivel
     */
    ClassificationSummary resumen(OffsetDateTime desde, OffsetDateTime hasta);
}
