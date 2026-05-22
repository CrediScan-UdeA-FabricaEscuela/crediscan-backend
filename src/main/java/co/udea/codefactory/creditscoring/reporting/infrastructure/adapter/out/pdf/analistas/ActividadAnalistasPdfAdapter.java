package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.pdf.analistas;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.GenerarActividadAnalistasPdfPort;

/**
 * Adaptador de salida que genera el reporte de actividad de analistas en PDF
 * usando la librería OpenPDF (com.lowagie).
 */
@Component
public class ActividadAnalistasPdfAdapter implements GenerarActividadAnalistasPdfPort {

    @Override
    public byte[] generar(ActividadAnalistasReporte r) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Título del reporte
            doc.add(new Paragraph("Reporte de Actividad de Analistas",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            doc.add(new Paragraph(" "));

            // Rango de fechas
            doc.add(new Paragraph("Desde: " + r.desde() + "  Hasta: " + r.hasta()));
            doc.add(new Paragraph(" "));

            if (!r.hasData()) {
                doc.add(new Paragraph("No hay evaluaciones con decisión en el rango especificado"));
            } else {
                // Estadísticas del equipo
                doc.add(new Paragraph("Estadísticas del Equipo",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                doc.add(new Paragraph("Total evaluaciones: "
                        + r.estadisticasEquipo().totalEvaluacionesEquipo()));
                doc.add(new Paragraph("Número de analistas: "
                        + r.estadisticasEquipo().numAnalistas()));
                doc.add(new Paragraph("Tiempo medio equipo (h hábiles): "
                        + String.format("%.2f", r.estadisticasEquipo().mediaEquipoHorasHabiles())));
                doc.add(new Paragraph("Desv. estándar (h hábiles): "
                        + String.format("%.2f", r.estadisticasEquipo().stddevHorasHabiles())));
                doc.add(new Paragraph(" "));

                // Tabla de analistas
                doc.add(new Paragraph("Detalle por Analista",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                PdfPTable tabla = new PdfPTable(5);
                tabla.setWidthPercentage(100);
                tabla.addCell("Analista");
                tabla.addCell("Total Eval.");
                tabla.addCell("% Aprobación");
                tabla.addCell("Tiempo Medio (h)");
                tabla.addCell("Outlier");

                for (var analista : r.analistas()) {
                    // nombre: fallback a username cuando no hay nombre de pila disponible (CA1-017)
                    tabla.addCell(analista.nombre());
                    tabla.addCell(String.valueOf(analista.totalEvaluaciones()));
                    tabla.addCell(analista.distribucion().pctAprobacion() + "%");
                    tabla.addCell(String.format("%.2f", analista.tiempoMedioHorasHabiles()));
                    tabla.addCell(analista.isOutlier() ? "Sí" : "No");
                }
                doc.add(tabla);
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error generando el PDF del reporte de actividad de analistas", e);
        }
    }
}
