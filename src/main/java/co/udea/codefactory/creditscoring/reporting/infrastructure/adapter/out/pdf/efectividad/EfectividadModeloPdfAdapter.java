package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.pdf.efectividad;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import co.udea.codefactory.creditscoring.reporting.domain.model.efectividad.EfectividadModeloReporte;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.efectividad.GenerarEfectividadPdfPort;

/**
 * Adaptador de salida que genera el reporte de efectividad del modelo en PDF
 * usando la librería OpenPDF (com.lowagie).
 */
@Component
public class EfectividadModeloPdfAdapter implements GenerarEfectividadPdfPort {

    @Override
    public byte[] generar(EfectividadModeloReporte r) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Título del reporte
            doc.add(new Paragraph("Reporte de Efectividad del Modelo de Riesgo",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            doc.add(new Paragraph(" "));

            // Rango de fechas
            doc.add(new Paragraph("Desde: " + r.desde() + "  Hasta: " + r.hasta()));
            if (r.analistaId() != null) {
                doc.add(new Paragraph("Analista: " + r.analistaId()));
            }
            doc.add(new Paragraph(" "));

            if (!r.hasData()) {
                doc.add(new Paragraph("No hay evaluaciones con decisión en el rango especificado"));
            } else {
                // Tabla de indicadores
                if (r.indicadores() != null) {
                    doc.add(new Paragraph("Indicadores de Efectividad",
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                    doc.add(new Paragraph("Tasa de concordancia global: "
                            + r.indicadores().tasaConcordanciaGlobal() + "%"));
                    doc.add(new Paragraph("Tasa de override (aprobación): "
                            + r.indicadores().tasaOverrideAprobacion() + "%"));
                    doc.add(new Paragraph("Tasa de override (rechazo): "
                            + r.indicadores().tasaOverrideRechazo() + "%"));
                    doc.add(new Paragraph("Total casos activos: "
                            + r.indicadores().totalCasos()));
                    doc.add(new Paragraph(" "));
                }

                // Matriz de confusión
                doc.add(new Paragraph("Matriz de Confusión",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                PdfPTable tabla = new PdfPTable(3);
                tabla.setWidthPercentage(100);
                tabla.addCell("Nivel de Riesgo");
                tabla.addCell("Decisión");
                tabla.addCell("Cantidad");

                for (var celda : r.matriz().celdas()) {
                    tabla.addCell(celda.riskLevel().name());
                    tabla.addCell(celda.decision().name());
                    tabla.addCell(String.valueOf(celda.count()));
                }
                doc.add(tabla);
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error generando el PDF del reporte de efectividad del modelo", e);
        }
    }
}
