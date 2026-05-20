package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;

/**
 * Tests unitarios de {@link CsvEvaluationListAdapter}.
 */
class CsvEvaluationListAdapterTest {

    private final CsvEvaluationListAdapter adapter = new CsvEvaluationListAdapter();

    @Test
    void csv_contieneHeadersEsperados() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        adapter.escribir(baos, Stream.empty());
        String csv = baos.toString("UTF-8");
        assertThat(csv).contains("solicitante", "fecha", "puntaje", "nivel", "decision", "analista");
    }

    @Test
    void csv_dos_items_generaDosFilasDatos() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Stream<EvaluationSearchItem> items = Stream.of(
                crearItem("Ana Perez", "APPROVED"),
                crearItem("Luis Gomez", "REJECTED"));
        adapter.escribir(baos, items);
        String csv = baos.toString("UTF-8");
        String[] lines = csv.split("\n");
        // Header + 2 data rows
        assertThat(lines.length).isGreaterThanOrEqualTo(3);
        assertThat(csv).contains("Ana Perez");
        assertThat(csv).contains("Luis Gomez");
    }

    @Test
    void csv_sinDecision_renderizaComoSinDecision() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Stream<EvaluationSearchItem> items = Stream.of(crearItem("Carlos Ruiz", null));
        adapter.escribir(baos, items);
        String csv = baos.toString("UTF-8");
        assertThat(csv).contains("SIN_DECISION");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private EvaluationSearchItem crearItem(String nombre, String decision) {
        return new EvaluationSearchItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                nombre,
                OffsetDateTime.parse("2025-03-15T10:00:00Z"),
                new BigDecimal("75.50"),
                RiskLevel.MEDIUM,
                decision,
                "analista1");
    }
}
