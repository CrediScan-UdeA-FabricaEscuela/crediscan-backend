package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.in.rest.dto;

/**
 * DTO de respuesta para el conteo de un nivel de riesgo específico.
 */
public record LevelCountDto(String nivelRiesgo, long cantidad) {
}
