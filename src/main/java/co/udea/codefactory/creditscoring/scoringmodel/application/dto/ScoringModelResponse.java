package co.udea.codefactory.creditscoring.scoringmodel.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Representación de una versión del modelo de scoring para la respuesta REST. */
public record ScoringModelResponse(
        UUID id,
        String nombre,
        String descripcion,
        int version,
        String estado,
        List<ModelVariableResponse> variables,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActivacion) {

    /** Variable incluida en la versión del modelo. */
    public record ModelVariableResponse(
            UUID id,
            UUID variableId,
            BigDecimal peso,
            String rangosSnapshot) {
    }
}
