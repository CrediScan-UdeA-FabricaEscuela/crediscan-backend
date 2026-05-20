package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;

/**
 * Tests unitarios de {@link PdfEvaluationListReportAdapter}.
 */
class PdfEvaluationListReportAdapterTest {

    private final PdfEvaluationListReportAdapter adapter = new PdfEvaluationListReportAdapter();

    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2025-06-30T23:59:59Z");

    @Test
    void pdf_byteArrayEmpieza_ConMagicBytes() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, null, null, null, null, null);
        byte[] pdf = adapter.generar(List.of(crearItem()), criteria);
        // Los primeros 4 bytes de un PDF válido son %PDF
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void pdf_con1Item_generaContenidoNoVacio() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, null, null, null, null, null);
        byte[] pdf = adapter.generar(List.of(crearItem()), criteria);
        assertThat(pdf.length).isGreaterThan(0);
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private EvaluationSearchItem crearItem() {
        return new EvaluationSearchItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Solicitante",
                OffsetDateTime.parse("2025-03-15T10:00:00Z"),
                new BigDecimal("75.00"),
                RiskLevel.MEDIUM,
                "APPROVED",
                "analista1");
    }
}
