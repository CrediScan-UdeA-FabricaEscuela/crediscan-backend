package co.udea.codefactory.creditscoring.scoringengine.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Detalle del puntaje calculado para una variable individual del modelo.
 * Parte del desglose incluido en el resultado del cálculo (CA6).
 */
public record VariableScoreDetail(
        UUID variableId,
        String nombreVariable,
        BigDecimal valorObservado,
        String etiquetaRango,
        int puntajeParcial,
        BigDecimal peso,
        BigDecimal contribucion
) {}
