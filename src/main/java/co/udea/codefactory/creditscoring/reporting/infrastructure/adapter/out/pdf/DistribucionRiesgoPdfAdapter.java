package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.pdf;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import co.udea.codefactory.creditscoring.reporting.domain.model.RiskDistributionReport;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.GenerarReportePdfPort;

/**
 * Adaptador de salida que genera el reporte de distribución de riesgo en PDF
 * usando la librería OpenPDF (com.lowagie).
 */
@Component
public class DistribucionRiesgoPdfAdapter implements GenerarReportePdfPort {

    @Override
    public byte[] generar(RiskDistributionReport r) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Título
            doc.add(new Paragraph("Reporte de Distribución de Riesgo",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            doc.add(new Paragraph(" "));

            // Rango de fechas
            doc.add(new Paragraph("Desde: " + r.fechaDesde() + "  Hasta: " + r.fechaHasta()));

            // Filtro tipo de empleo (opcional)
            if (r.tipoEmpleo() != null) {
                doc.add(new Paragraph("Tipo de empleo: " + r.tipoEmpleo()));
            }

            doc.add(new Paragraph(" "));

            if (!r.hasData()) {
                doc.add(new Paragraph("No hay evaluaciones en el rango especificado"));
            } else {
                // Tabla resumen por nivel
                PdfPTable tabla = new PdfPTable(4);
                tabla.setWidthPercentage(100);
                tabla.addCell("Nivel");
                tabla.addCell("Cantidad");
                tabla.addCell("Porcentaje");
                tabla.addCell("Score promedio");

                for (var row : r.tabla()) {
                    tabla.addCell(row.level().name());
                    tabla.addCell(String.valueOf(row.count()));
                    tabla.addCell(row.percentage() + "%");
                    tabla.addCell(row.averageScore().toString());
                }

                doc.add(tabla);
                doc.add(new Paragraph(" "));

                // Estadísticas globales
                doc.add(new Paragraph("Total evaluaciones: " + r.overall().totalEvaluations()));
                doc.add(new Paragraph("Score promedio: " + r.overall().averageScore()));
                doc.add(new Paragraph("Desviación estándar: " + r.overall().stdDev()));
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("Error generando el PDF del reporte de distribución de riesgo", e);
        }
    }
}
