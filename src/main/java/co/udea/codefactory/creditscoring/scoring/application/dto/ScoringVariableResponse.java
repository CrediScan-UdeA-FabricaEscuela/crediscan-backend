package co.udea.codefactory.creditscoring.scoring.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Representación de una variable de scoring para la respuesta REST. */
public record ScoringVariableResponse(
        UUID id,
        String nombre,
        String descripcion,
        String tipo,
        BigDecimal peso,
        boolean activa,
        List<RangoResponse> rangos,
        List<CategoriaResponse> categorias) {

    /** Rango numérico de una variable de scoring. */
    public record RangoResponse(
            UUID id,
            BigDecimal limiteInferior,
            BigDecimal limiteSuperior,
            int puntaje,
            String etiqueta) {
    }

    /** Categoría de una variable de scoring. */
    public record CategoriaResponse(
            UUID id,
            String categoria,
            int puntaje,
            String etiqueta) {
    }
}
