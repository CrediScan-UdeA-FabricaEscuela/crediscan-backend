package co.udea.codefactory.creditscoring.evaluation.domain.model;

import java.util.UUID;

/**
 * Información básica de un modelo de scoring, retornada por {@code ScoringModelQueryPort}.
 */
public record ModelInfo(UUID id, String name, int version) {
}
