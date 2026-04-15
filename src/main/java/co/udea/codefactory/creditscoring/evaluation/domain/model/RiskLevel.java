package co.udea.codefactory.creditscoring.evaluation.domain.model;

import java.math.BigDecimal;

/**
 * Nivel de riesgo crediticio derivado del puntaje total.
 * REJECTED se usa exclusivamente cuando el solicitante fue rechazado por una regla knockout.
 */
public enum RiskLevel {

    VERY_LOW(85, 100),
    LOW(70, 84),
    MEDIUM(50, 69),
    HIGH(30, 49),
    VERY_HIGH(0, 29),
    REJECTED(-1, -1);

    private final int minScore;
    private final int maxScore;

    RiskLevel(int min, int max) {
        this.minScore = min;
        this.maxScore = max;
    }

    /**
     * Clasifica el puntaje en el nivel de riesgo correspondiente.
     * El puntaje debe estar entre 0 y 100 inclusive.
     */
    public static RiskLevel fromScore(BigDecimal score) {
        if (score == null) {
            throw new IllegalArgumentException("El puntaje no puede ser nulo");
        }
        int s = score.intValue();
        if (s < 0 || s > 100) {
            throw new IllegalArgumentException(
                    "El puntaje debe estar entre 0 y 100, recibido: " + score);
        }
        if (s >= 85) return VERY_LOW;
        if (s >= 70) return LOW;
        if (s >= 50) return MEDIUM;
        if (s >= 30) return HIGH;
        return VERY_HIGH;
    }

    /** Retorna el nivel REJECTED para solicitantes rechazados por knockout. */
    public static RiskLevel rejected() {
        return REJECTED;
    }

    public int getMinScore() { return minScore; }
    public int getMaxScore() { return maxScore; }
}
