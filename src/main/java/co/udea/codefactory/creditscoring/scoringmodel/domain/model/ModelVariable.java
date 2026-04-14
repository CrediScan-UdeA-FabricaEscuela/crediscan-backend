package co.udea.codefactory.creditscoring.scoringmodel.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.domain.exception.ScoringModelValidationException;

/**
 * Variable de scoring incluida en una versión del modelo.
 * El campo {@code rangosSnapshot} es nulo mientras el modelo está en borrador;
 * se completa con una representación JSON en el momento de la activación (CA snapshot).
 */
public record ModelVariable(
        UUID id,
        UUID modeloId,
        UUID variableId,
        BigDecimal peso,
        String rangosSnapshot) {

    public ModelVariable {
        if (variableId == null) {
            throw new ScoringModelValidationException("El identificador de la variable es obligatorio");
        }
        if (peso == null || peso.compareTo(BigDecimal.ZERO) <= 0 || peso.compareTo(BigDecimal.ONE) > 0) {
            throw new ScoringModelValidationException(
                    "El peso de la variable en el modelo debe ser mayor que 0 y menor o igual a 1 (recibido: " + peso + ")");
        }
    }
}
