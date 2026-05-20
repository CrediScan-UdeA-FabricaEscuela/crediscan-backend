package co.udea.codefactory.creditscoring.evaluation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.udea.codefactory.creditscoring.evaluation.domain.exception.ExportLimitExceededException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.ExportFormat;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.ExportEvaluationsUseCase.ExportArtifact;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationListReportPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.shared.PagedResult;

/**
 * Tests unitarios de {@link ExportEvaluationsService}.
 */
class ExportEvaluationsServiceTest {

    private EvaluationRepositoryPort repo;
    private EvaluationListReportPort pdfPort;
    private ExportEvaluationsService service;

    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2025-01-01T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2025-06-30T23:59:59Z");

    @BeforeEach
    void setUp() {
        repo = mock(EvaluationRepositoryPort.class);
        pdfPort = mock(EvaluationListReportPort.class);
        service = new ExportEvaluationsService(repo, pdfPort);
    }

    @Test
    void pdf_countMayorA1000_lanzaExportLimitExceededException() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, null, null, null, null, null);
        when(repo.countByCriteria(criteria)).thenReturn(1500L);

        assertThatThrownBy(() -> service.export(criteria, ExportFormat.PDF))
                .isInstanceOf(ExportLimitExceededException.class);
        verify(pdfPort, never()).generar(anyList(), any());
    }

    @Test
    void pdf_countMenorA1000_invocaRepoYPdfPort() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, null, null, null, null, null);
        when(repo.countByCriteria(criteria)).thenReturn(50L);
        PagedResult<EvaluationSearchItem> page = new PagedResult<>(List.of(), 0, 0, 0, 1000);
        when(repo.search(any(), any())).thenReturn(page);
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
        when(pdfPort.generar(anyList(), any())).thenReturn(pdfBytes);

        ExportArtifact artifact = service.export(criteria, ExportFormat.PDF);

        assertThat(artifact).isNotNull();
        assertThat(artifact.contentType()).isEqualTo("application/pdf");
        assertThat(artifact.payload()).isEqualTo(pdfBytes);
        verify(pdfPort).generar(anyList(), any());
    }

    @Test
    void csv_lanzaUnsupportedOperationException() {
        EvaluationSearchCriteria criteria = new EvaluationSearchCriteria(
                DESDE, HASTA, null, null, null, null, null);

        assertThatThrownBy(() -> service.export(criteria, ExportFormat.CSV))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
