package co.udea.codefactory.creditscoring.scoringengine.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Resultado completo del cálculo del puntaje crediticio para un solicitante (CA6).
 *
 * <p>Si {@code rechazadoPorKo} es {@code true}, el puntaje final no es significativo
 * y el motivo de rechazo se incluye en {@code mensajeKo}. Las reglas evaluadas
 * se listan siempre para trazabilidad (CA6).</p>
 */
public record ScoringResult(
        UUID modeloId,
        UUID aplicanteId,
        BigDecimal puntajeFinal,
        List<VariableScoreDetail> desglose,
        List<KnockoutEvaluationDetail> reglasKoEvaluadas,
        boolean rechazadoPorKo,
        String mensajeKo
) {

    /** Construye un resultado de rechazo por knockout. */
    public static ScoringResult rechazado(
            UUID modeloId, UUID aplicanteId,
            List<KnockoutEvaluationDetail> reglasEvaluadas,
            String mensajeKo) {
        return new ScoringResult(
                modeloId, aplicanteId,
                BigDecimal.ZERO,
                List.of(),
                reglasEvaluadas,
                true, mensajeKo);
    }

    /** Construye un resultado aprobado con el puntaje calculado. */
    public static ScoringResult aprobado(
            UUID modeloId, UUID aplicanteId,
            BigDecimal puntajeFinal,
            List<VariableScoreDetail> desglose,
            List<KnockoutEvaluationDetail> reglasEvaluadas) {
        return new ScoringResult(
                modeloId, aplicanteId,
                puntajeFinal, desglose,
                reglasEvaluadas,
                false, null);
    }
}
