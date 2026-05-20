package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import java.io.OutputStream;
import java.util.stream.Stream;

import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;

/**
 * Puerto de salida: escritura streaming del listado de evaluaciones en formato CSV.
 *
 * <p>Escribe directamente al {@link OutputStream} para evitar acumular datos en memoria.
 * Usado con {@code StreamingResponseBody} en el controlador REST.</p>
 */
public interface EvaluationListCsvPort {

    /**
     * Escribe el CSV directamente al stream de salida.
     *
     * @param out   stream de destino (del {@code HttpServletResponse})
     * @param items stream de evaluaciones a serializar
     */
    void escribir(OutputStream out, Stream<EvaluationSearchItem> items);
}
