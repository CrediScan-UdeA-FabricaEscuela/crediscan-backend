package co.udea.codefactory.creditscoring.scoringmodel.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.domain.exception.ScoringModelValidationException;

/**
 * Regla de exclusión automática (knock-out) asociada a un modelo de scoring.
 *
 * <p>Se evalúa ANTES del cálculo del puntaje (RN2). Si el valor observado del campo
 * configurado satisface la condición {@code operador(valorObservado, umbral)},
 * la solicitud es rechazada automáticamente sin calcular el score (CA3).</p>
 *
 * <p>Las reglas se evalúan en orden ascendente de {@code prioridad} (CA5, RN5).</p>
 */
public record KnockoutRule(
        UUID id,
        UUID modeloId,
        String campo,
        KnockoutOperator operador,
        BigDecimal umbral,
        String mensaje,
        int prioridad,
        boolean activa) {

    public KnockoutRule {
        if (modeloId == null) {
            throw new ScoringModelValidationException("El identificador del modelo es obligatorio en una regla knockout");
        }
        if (campo == null || campo.isBlank()) {
            throw new ScoringModelValidationException("El campo de referencia de la regla knockout es obligatorio");
        }
        if (operador == null) {
            throw new ScoringModelValidationException("El operador de comparación es obligatorio");
        }
        if (umbral == null) {
            throw new ScoringModelValidationException("El valor umbral de la regla knockout es obligatorio");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new ScoringModelValidationException("El mensaje descriptivo de la regla knockout es obligatorio");
        }
        if (prioridad < 0) {
            throw new ScoringModelValidationException("La prioridad no puede ser negativa");
        }
    }

    // -------------------------------------------------------------------------
    // Métodos de fábrica
    // -------------------------------------------------------------------------

    public static KnockoutRule crear(
            UUID modeloId, String campo, KnockoutOperator operador,
            BigDecimal umbral, String mensaje, int prioridad) {
        return new KnockoutRule(
                UUID.randomUUID(), modeloId, campo, operador,
                umbral, mensaje, prioridad, true);
    }

    public static KnockoutRule rehydrate(
            UUID id, UUID modeloId, String campo, KnockoutOperator operador,
            BigDecimal umbral, String mensaje, int prioridad, boolean activa) {
        return new KnockoutRule(id, modeloId, campo, operador, umbral, mensaje, prioridad, activa);
    }

    // -------------------------------------------------------------------------
    // Lógica de evaluación
    // -------------------------------------------------------------------------

    /**
     * Evalúa si el valor observado activa esta regla.
     * Si la regla no está activa, siempre retorna {@code false}.
     *
     * @param valorObservado valor del campo del solicitante
     * @return {@code true} si la regla se activa (rechazo automático)
     */
    public boolean evaluar(BigDecimal valorObservado) {
        if (!activa) {
            return false;
        }
        return operador.evaluar(valorObservado, umbral);
    }

    // -------------------------------------------------------------------------
    // Mutaciones
    // -------------------------------------------------------------------------

    public KnockoutRule desactivar() {
        return new KnockoutRule(id, modeloId, campo, operador, umbral, mensaje, prioridad, false);
    }
}
