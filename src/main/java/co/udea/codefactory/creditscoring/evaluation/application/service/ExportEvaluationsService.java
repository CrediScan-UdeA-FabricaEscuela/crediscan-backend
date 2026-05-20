package co.udea.codefactory.creditscoring.evaluation.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.evaluation.domain.exception.ExportLimitExceededException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.ExportFormat;
import co.udea.codefactory.creditscoring.evaluation.domain.port.in.ExportEvaluationsUseCase;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationListReportPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationRepositoryPort;
import co.udea.codefactory.creditscoring.shared.PageRequest;

/**
 * Servicio de aplicación para exportar el listado filtrado de evaluaciones en PDF.
 *
 * <p>El export CSV es responsabilidad del controlador (streaming directo).
 * Este servicio sólo maneja PDF (buffered, capped a 1000 filas).</p>
 */
@Service
public class ExportEvaluationsService implements ExportEvaluationsUseCase {

    private static final long PDF_MAX_ROWS = 1000L;

    private final EvaluationRepositoryPort repo;
    private final EvaluationListReportPort pdfPort;

    public ExportEvaluationsService(EvaluationRepositoryPort repo,
                                    EvaluationListReportPort pdfPort) {
        this.repo = repo;
        this.pdfPort = pdfPort;
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
        // El export CSV se delega al controller via StreamingResponseBody.
        throw new UnsupportedOperationException("CSV streaming delegado al controller");
    }
}
