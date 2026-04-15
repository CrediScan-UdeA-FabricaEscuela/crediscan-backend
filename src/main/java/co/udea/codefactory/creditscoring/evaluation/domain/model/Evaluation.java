package co.udea.codefactory.creditscoring.evaluation.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Agregado raíz del contexto de evaluación crediticia.
 *
 * <p>Una evaluación representa el resultado completo del proceso de scoring para
 * un solicitante en un momento dado. Es inmutable una vez creada.</p>
 *
 * <p>Nota: en caso de rechazo por KO, el puntaje es BigDecimal.ZERO
 * y el riskLevel es REJECTED.</p>
 */
public record Evaluation(
        UUID id,
        UUID applicantId,
        UUID modelId,
        UUID financialDataId,
        BigDecimal totalScore,
        RiskLevel riskLevel,
        boolean knockedOut,
        String knockoutReasons,
        OffsetDateTime evaluatedAt,
        String evaluatedBy,
        OffsetDateTime createdAt,
        String createdBy,
        List<EvaluationDetail> details,
        List<EvaluationKnockout> knockouts
) {

    public Evaluation {
        if (id == null) throw new IllegalArgumentException("El id de la evaluacion es obligatorio");
        if (applicantId == null) throw new IllegalArgumentException("El applicantId es obligatorio");
        if (modelId == null) throw new IllegalArgumentException("El modelId es obligatorio");
        if (financialDataId == null) throw new IllegalArgumentException("El financialDataId es obligatorio");
        if (totalScore == null
                || totalScore.compareTo(BigDecimal.ZERO) < 0
                || totalScore.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El puntaje total debe estar entre 0 y 100");
        }
        if (riskLevel == null) throw new IllegalArgumentException("El nivel de riesgo es obligatorio");
        if (evaluatedBy == null || evaluatedBy.isBlank())
            throw new IllegalArgumentException("El evaluador es obligatorio");
        if (createdAt == null) throw new IllegalArgumentException("La fecha de creacion es obligatoria");
        if (createdBy == null || createdBy.isBlank())
            throw new IllegalArgumentException("El creador es obligatorio");
        // Copia defensiva de las listas
        details = details != null ? List.copyOf(details) : List.of();
        knockouts = knockouts != null ? List.copyOf(knockouts) : List.of();
    }

    /**
     * Factory method para crear una nueva evaluación.
     * Genera id y timestamps automáticamente.
     */
    public static Evaluation crear(UUID applicantId, UUID modelId, UUID financialDataId,
            BigDecimal totalScore, RiskLevel riskLevel, boolean knockedOut, String knockoutReasons,
            String evaluatedBy, List<EvaluationDetail> details, List<EvaluationKnockout> knockouts) {
        OffsetDateTime ahora = OffsetDateTime.now();
        return new Evaluation(UUID.randomUUID(), applicantId, modelId, financialDataId,
                totalScore, riskLevel, knockedOut, knockoutReasons,
                ahora, evaluatedBy, ahora, evaluatedBy, details, knockouts);
    }

    /**
     * Factory method para reconstruir una evaluación desde persistencia.
     * No genera ids ni timestamps nuevos.
     */
    public static Evaluation rehydrate(UUID id, UUID applicantId, UUID modelId, UUID financialDataId,
            BigDecimal totalScore, RiskLevel riskLevel, boolean knockedOut, String knockoutReasons,
            OffsetDateTime evaluatedAt, String evaluatedBy, OffsetDateTime createdAt, String createdBy,
            List<EvaluationDetail> details, List<EvaluationKnockout> knockouts) {
        return new Evaluation(id, applicantId, modelId, financialDataId,
                totalScore, riskLevel, knockedOut, knockoutReasons,
                evaluatedAt, evaluatedBy, createdAt, createdBy, details, knockouts);
    }
}
