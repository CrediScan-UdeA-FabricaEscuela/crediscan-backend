package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;

/**
 * Puerto de salida para la generación de reportes de evaluación.
 * Desacopla la generación de PDF de la lógica de dominio.
 */
public interface EvaluationReportPort {

    /**
     * Genera el reporte PDF de la evaluación dada.
     *
     * @param evaluation la evaluación a incluir en el reporte
     * @return bytes del documento PDF generado
     */
    byte[] generar(Evaluation evaluation);
}
