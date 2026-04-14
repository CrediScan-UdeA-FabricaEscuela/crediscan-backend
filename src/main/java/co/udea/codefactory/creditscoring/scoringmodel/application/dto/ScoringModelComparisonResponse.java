package co.udea.codefactory.creditscoring.scoringmodel.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Resultado de comparar dos versiones del modelo de scoring (CA6). */
public record ScoringModelComparisonResponse(
        ScoringModelResponse modeloBase,
        ScoringModelResponse modeloComparado,
        List<DiferenciaVariable> diferencias) {

    /**
     * Diferencia en una variable entre las dos versiones.
     * El tipo puede ser: AGREGADA, ELIMINADA, MODIFICADA, SIN_CAMBIO.
     */
    public record DiferenciaVariable(
            UUID variableId,
            String tipo,
            BigDecimal pesoBase,
            BigDecimal pesoComparado) {
    }
}
