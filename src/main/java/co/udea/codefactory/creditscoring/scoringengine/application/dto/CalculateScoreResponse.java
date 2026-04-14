package co.udea.codefactory.creditscoring.scoringengine.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CalculateScoreResponse(
        UUID modeloId,
        UUID aplicanteId,
        BigDecimal puntajeFinal,
        List<VariableDetailDto> desglose,
        List<KoEvaluacionDto> reglasKoEvaluadas,
        boolean rechazadoPorKo,
        String mensajeKo
) {

    public record VariableDetailDto(
            UUID variableId,
            String nombreVariable,
            BigDecimal valorObservado,
            String etiquetaRango,
            int puntajeParcial,
            BigDecimal peso,
            BigDecimal contribucion
    ) {}

    public record KoEvaluacionDto(
            UUID reglaId,
            String campo,
            String operador,
            BigDecimal umbral,
            BigDecimal valorObservado,
            boolean activada,
            String mensaje
    ) {}
}
