package co.udea.codefactory.creditscoring.evaluation.domain.model;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Resumen de clasificación de solicitantes por nivel de riesgo.
 * Siempre contiene los 6 niveles de riesgo aunque el conteo sea 0.
 */
public record ClassificationSummary(
        List<LevelCount> levels,
        OffsetDateTime desde,
        OffsetDateTime hasta) {

    /**
     * Construye el resumen de clasificación a partir de un mapa de conteos.
     * Itera sobre todos los valores de {@link RiskLevel} y rellena con 0
     * los niveles ausentes en el mapa para garantizar siempre los 6 niveles.
     *
     * @param counts mapa con conteos por nivel (puede estar vacío o parcial)
     * @param desde  inicio del rango de fecha consultado
     * @param hasta  fin del rango de fecha consultado
     * @return resumen con exactamente 6 entradas de nivel
     */
    public static ClassificationSummary crear(Map<RiskLevel, Long> counts,
            OffsetDateTime desde, OffsetDateTime hasta) {
        List<LevelCount> levels = Arrays.stream(RiskLevel.values())
                .map(level -> new LevelCount(level, counts.getOrDefault(level, 0L)))
                .toList();
        return new ClassificationSummary(levels, desde, hasta);
    }
}
