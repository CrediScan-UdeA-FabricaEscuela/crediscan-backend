package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO de respuesta para el resumen de clasificación de riesgo del portafolio.
 */
public record ClassificationSummaryResponse(
        List<LevelCountDto> niveles,
        OffsetDateTime desde,
        OffsetDateTime hasta) {
}
