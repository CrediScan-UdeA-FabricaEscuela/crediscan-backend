package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.csv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.reporting.domain.model.analistas.ActividadAnalistasReporte;
import co.udea.codefactory.creditscoring.reporting.domain.port.out.analistas.GenerarActividadAnalistasCSVPort;

/**
 * Adaptador de salida que genera el reporte de actividad de analistas en CSV
 * usando Apache Commons CSV.
 */
@Component
public class ActividadAnalistasCSVAdapter implements GenerarActividadAnalistasCSVPort {

    /** Cabeceras del CSV. */
    private static final String[] CSV_HEADERS = {
        "analistaId",
        "nombre",
        "totalEvaluaciones",
        "aprobadas",
        "rechazadas",
        "revisionManual",
        "escaladas",
        "tasaAprobacion",
        "tasaRechazo",
        "tasaManual",
        "tasaEscalado",
        "tiempoMedioHorasHabiles",
        "isOutlier"
    };

    @Override
    public byte[] generar(ActividadAnalistasReporte r) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(CSV_HEADERS)
                     .build())) {

            // Si no hay datos, se incluye solo la fila de cabecera
            for (var analista : r.analistas()) {
                printer.printRecord(
                        analista.evaluatedBy(),
                        analista.nombre(),
                        analista.totalEvaluaciones(),
                        analista.distribucion().aprobadas(),
                        analista.distribucion().rechazadas(),
                        analista.distribucion().revisionManual(),
                        analista.distribucion().escaladas(),
                        analista.distribucion().pctAprobacion().toPlainString(),
                        analista.distribucion().pctRechazo().toPlainString(),
                        analista.distribucion().pctManual().toPlainString(),
                        analista.distribucion().pctEscalado().toPlainString(),
                        String.format("%.2f", analista.tiempoMedioHorasHabiles()),
                        analista.isOutlier());
            }

            printer.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new UncheckedIOException("Error generando el CSV de actividad de analistas", e);
        }
    }
}
