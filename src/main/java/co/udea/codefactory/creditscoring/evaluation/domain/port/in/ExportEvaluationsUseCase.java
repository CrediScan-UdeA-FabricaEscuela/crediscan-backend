package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.ExportFormat;

/**
 * Puerto de entrada: exportación del listado filtrado de evaluaciones (PDF buffered).
 *
 * <p>Nota: el export CSV se realiza vía streaming directo desde el controller;
 * este use case sólo es responsable del formato PDF (buffered, capped a 1000 filas).</p>
 */
public interface ExportEvaluationsUseCase {

    /**
     * Artefacto de exportación generado (filename + contentType + payload en bytes).
     */
    record ExportArtifact(String filename, String contentType, byte[] payload) {}

    /**
     * Exporta el listado filtrado en el formato indicado.
     *
     * @param criteria criterios de filtrado
     * @param format   formato de exportación ({@link ExportFormat#PDF} soportado; CSV delega al controller)
     * @return artefacto listo para enviar al cliente
     * @throws co.udea.codefactory.creditscoring.evaluation.domain.exception.ExportLimitExceededException
     *         si el formato es PDF y el número de filas supera 1000
     */
    ExportArtifact export(EvaluationSearchCriteria criteria, ExportFormat format);
}
