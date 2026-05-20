package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.evaluation.domain.model.ModelInfo;

/**
 * Puerto de salida para consultas de modelos de scoring desde el BC de evaluación.
 * Evita dependencia directa con la capa de persistencia del BC scoringmodel.
 */
public interface ScoringModelQueryPort {

    /**
     * Busca la información básica de un modelo de scoring por su identificador.
     *
     * @param modelId identificador único del modelo
     * @return {@link Optional} con la información del modelo, vacío si no existe
     */
    Optional<ModelInfo> findById(UUID modelId);
}
