package co.udea.codefactory.creditscoring.evaluation.domain.model;

/**
 * Vista enriquecida de una evaluación, incluyendo nombre del modelo,
 * versión y nombre del solicitante.
 */
public record EvaluationDetailView(
        Evaluation evaluation,
        String modelName,
        int modelVersion,
        String applicantName) {
}
