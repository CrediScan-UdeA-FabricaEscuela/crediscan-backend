package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.csv;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationListCsvPort;

/**
 * Adaptador de salida: escribe streaming CSV del listado de evaluaciones
 * usando Apache Commons CSV.
 */
@Component
public class CsvEvaluationListAdapter implements EvaluationListCsvPort {

    private static final String[] HEADERS = {
        "solicitante", "fecha", "puntaje", "nivel", "decision", "analista"
    };

    @Override
    public void escribir(OutputStream out, Stream<EvaluationSearchItem> items) {
        try (var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             var printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(HEADERS)
                     .build())) {
            items.forEach(item -> {
                try {
                    printer.printRecord(
                            item.applicantName(),
                            item.evaluatedAt().toString(),
                            item.score().toPlainString(),
                            item.riskLevel().name(),
                            item.decisionStatus() != null ? item.decisionStatus() : "SIN_DECISION",
                            item.analista());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
