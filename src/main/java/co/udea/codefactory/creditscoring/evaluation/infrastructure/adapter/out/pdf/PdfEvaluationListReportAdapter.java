package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.pdf;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationListReportPort;

/**
 * Adaptador de salida: genera reporte PDF del listado de evaluaciones usando OpenPDF.
 * Formato landscape (A4 rotado) para acomodar las 6 columnas del listado.
 */
@Component
public class PdfEvaluationListReportAdapter implements EvaluationListReportPort {

    @Override
    public byte[] generar(List<EvaluationSearchItem> items, EvaluationSearchCriteria criteria) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font bodyFont   = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // Título
            Paragraph titulo = new Paragraph("Listado de Evaluaciones Crediticias", titleFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(10);
            doc.add(titulo);

            // Rango de fechas en encabezado
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            doc.add(new Paragraph("Período: " + criteria.fechaDesde().toLocalDate()
                    + " — " + criteria.fechaHasta().toLocalDate(), infoFont));
            doc.add(new Paragraph("Total de registros: " + items.size(), infoFont));
            doc.add(new Paragraph(" "));

            // Tabla de resultados
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2.5f, 1.5f, 1.5f, 2f, 2f});

            // Cabeceras
            for (String h : List.of("Solicitante", "Fecha", "Puntaje", "Nivel", "Decisión", "Analista")) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new java.awt.Color(220, 220, 220));
                table.addCell(cell);
            }

            // Filas de datos
            for (EvaluationSearchItem item : items) {
                table.addCell(new Phrase(item.applicantName() != null ? item.applicantName() : "", bodyFont));
                table.addCell(new Phrase(item.evaluatedAt().toLocalDate().toString(), bodyFont));
                table.addCell(new Phrase(item.score() != null ? item.score().toPlainString() : "", bodyFont));
                table.addCell(new Phrase(item.riskLevel().name(), bodyFont));
                table.addCell(new Phrase(
                        item.decisionStatus() != null ? item.decisionStatus() : "SIN_DECISION", bodyFont));
                table.addCell(new Phrase(item.analista() != null ? item.analista() : "", bodyFont));
            }

            doc.add(table);
            doc.close();
            return baos.toByteArray();
        } catch (com.lowagie.text.DocumentException | java.io.IOException e) {
            throw new IllegalStateException("Error al generar el PDF del listado de evaluaciones", e);
        }
    }
}
