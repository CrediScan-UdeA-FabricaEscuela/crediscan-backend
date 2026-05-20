package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto;

import java.util.List;

/**
 * DTO de respuesta paginada para la búsqueda avanzada de evaluaciones.
 */
public record EvaluationSearchResponse(
        List<EvaluationSearchItemDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
