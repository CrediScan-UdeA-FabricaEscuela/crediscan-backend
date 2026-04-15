package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.pdf;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationDetail;
import co.udea.codefactory.creditscoring.evaluation.domain.model.EvaluationKnockout;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.EvaluationReportPort;

/**
 * Adaptador de salida que genera reportes PDF de evaluaciones crediticias
 * usando la librería OpenPDF (fork de iText).
 */
@Component
public class PdfEvaluationReportAdapter implements EvaluationReportPort {

    @Override
    public byte[] generar(Evaluation evaluation) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Título principal
            Paragraph titulo = new Paragraph("Reporte de Evaluación de Riesgo Crediticio", titleFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(15);
            doc.add(titulo);

            // Información general de la evaluación
            doc.add(new Paragraph("Información General", headerFont));
            doc.add(new Paragraph("ID Evaluación: " + evaluation.id(), bodyFont));
            doc.add(new Paragraph("Solicitante: " + evaluation.applicantId(), bodyFont));
            doc.add(new Paragraph("Puntaje Total: " + evaluation.totalScore(), bodyFont));
            doc.add(new Paragraph("Nivel de Riesgo: " + evaluation.riskLevel(), bodyFont));
            doc.add(new Paragraph("Rechazado por KO: " + (evaluation.knockedOut() ? "Sí" : "No"), bodyFont));
            if (evaluation.knockoutReasons() != null) {
                doc.add(new Paragraph("Motivo: " + evaluation.knockoutReasons(), bodyFont));
            }
            doc.add(new Paragraph("Evaluado por: " + evaluation.evaluatedBy(), bodyFont));
            doc.add(new Paragraph("Fecha: " + evaluation.evaluatedAt(), bodyFont));

            // Tabla de desglose por variable de scoring
            if (!evaluation.details().isEmpty()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Desglose por Variable", headerFont));
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                for (String h : List.of("Variable", "Valor", "Puntaje", "Peso", "Ponderado")) {
                    PdfPCell cell = new PdfPCell(
                            new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
                    table.addCell(cell);
                }
                for (EvaluationDetail d : evaluation.details()) {
                    table.addCell(new Phrase(d.variableName(), bodyFont));
                    table.addCell(new Phrase(d.rawValue() != null ? d.rawValue() : "", bodyFont));
                    table.addCell(new Phrase(String.valueOf(d.score()), bodyFont));
                    table.addCell(new Phrase(String.valueOf(d.weight()), bodyFont));
                    table.addCell(new Phrase(String.valueOf(d.weightedScore()), bodyFont));
                }
                doc.add(table);
            }

            // Tabla de reglas knockout evaluadas
            if (!evaluation.knockouts().isEmpty()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Reglas Knockout Evaluadas", headerFont));
                PdfPTable koTable = new PdfPTable(3);
                koTable.setWidthPercentage(100);
                for (String h : List.of("Regla", "Valor del Campo", "Activada")) {
                    koTable.addCell(new PdfPCell(
                            new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9))));
                }
                for (EvaluationKnockout ko : evaluation.knockouts()) {
                    koTable.addCell(new Phrase(ko.ruleName(), bodyFont));
                    koTable.addCell(new Phrase(ko.fieldValue() != null ? ko.fieldValue() : "", bodyFont));
                    koTable.addCell(new Phrase(ko.triggered() ? "SÍ" : "NO", bodyFont));
                }
                doc.add(koTable);
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de la evaluación", e);
        }
    }
}
