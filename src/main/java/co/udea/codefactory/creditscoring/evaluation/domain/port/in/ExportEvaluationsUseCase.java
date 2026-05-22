package co.udea.codefactory.creditscoring.evaluation.domain.port.in;

import java.util.Arrays;
import java.util.Objects;

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
     * Las implementaciones de {@code equals}/{@code hashCode}/{@code toString}
     * consideran el contenido del array, no su referencia (Sonar S6218).
     */
    record ExportArtifact(String filename, String contentType, byte[] payload) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExportArtifact(var otherFilename, var otherContentType, var otherPayload))) return false;
            return Objects.equals(filename, otherFilename)
                && Objects.equals(contentType, otherContentType)
                && Arrays.equals(payload, otherPayload);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filename, contentType, Arrays.hashCode(payload));
        }

        @Override
        public String toString() {
            return "ExportArtifact[filename=" + filename
                + ", contentType=" + contentType
                + ", payload=" + Arrays.toString(payload) + "]";
        }
    }

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
