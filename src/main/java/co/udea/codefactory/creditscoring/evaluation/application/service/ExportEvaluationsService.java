package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.evaluation.domain.exception.ExportLimitExceededException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.ExportFormat;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.ExportEvaluationsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationListCsvPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationListReportPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.shared.PageRequest;

/**
 * Servicio de aplicación para exportar el listado filtrado de evaluaciones en PDF o CSV.
 */
@Service
public class ExportEvaluationsService implements ExportEvaluationsUseCase {

    private static final long PDF_MAX_ROWS = 1000L;
    private static final int CSV_MAX_ROWS = 10_000;

    private final EvaluationRepositoryPort repo;
    private final EvaluationListReportPort pdfPort;
    private final EvaluationListCsvPort csvPort;

    public ExportEvaluationsService(EvaluationRepositoryPort repo,
                                    EvaluationListReportPort pdfPort,
                                    EvaluationListCsvPort csvPort) {
        this.repo = repo;
        this.pdfPort = pdfPort;
        this.csvPort = csvPort;
    }

    @Override
    public ExportArtifact export(EvaluationSearchCriteria criteria, ExportFormat format) {
        if (format == ExportFormat.PDF) {
            long count = repo.countByCriteria(criteria);
            if (count > PDF_MAX_ROWS) {
                throw new ExportLimitExceededException(count, PDF_MAX_ROWS);
            }
            List<EvaluationSearchItem> items = repo
                    .search(criteria, new PageRequest(0, (int) PDF_MAX_ROWS))
                    .content();
            byte[] pdf = pdfPort.generar(items, criteria);
            return new ExportArtifact("evaluaciones.pdf", "application/pdf", pdf);
        }
        throw new UnsupportedOperationException("Formato no soportado: " + format);
    }

    @Override
    public byte[] exportCsv(EvaluationSearchCriteria criteria) {
        List<EvaluationSearchItem> items = repo
                .search(criteria, new PageRequest(0, CSV_MAX_ROWS))
                .content();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        csvPort.escribir(baos, items.stream());
        return baos.toByteArray();
    }
}
