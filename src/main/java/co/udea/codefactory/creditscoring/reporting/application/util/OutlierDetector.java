package co.udea.codefactory.creditscoring.reporting.application.util;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detector de outliers para tiempos de decisión de analistas.
 * <p>
 * Usa desviación estándar <b>poblacional</b> (no muestral) — RN2-017.
 * Un analista es outlier si {@code |tiempo - media| > 2 * stddev}.
 * </p>
 * <p>
 * Si N < 3, la detección se omite ({@code skipped=true}) y ningún analista es marcado.
 * Si {@code stddev == 0} (todos con el mismo tiempo), tampoco hay outliers.
 * </p>
 */
public final class OutlierDetector {

    private OutlierDetector() {
        // utilidad estática — no instanciar
    }

    /**
     * Detecta outliers en el mapa de {@code analistaId → tiempoMedioHorasHabiles}.
     *
     * @param tiempos mapa de identificador de analista a tiempo medio en horas hábiles
     * @return resultado con los IDs de outliers y la bandera {@code skipped}
     */
    public static OutlierResult detect(Map<String, Double> tiempos) {
        if (tiempos.size() < 3) {
            return new OutlierResult(Set.of(), true);
        }

        double[] valores = tiempos.values().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        double media  = mean(valores);
        double stddev = populationStddev(valores, media);

        if (stddev == 0.0) {
            return new OutlierResult(Set.of(), false);
        }

        double umbral = 2.0 * stddev;

        Set<String> outliers = tiempos.entrySet().stream()
                .filter(e -> Math.abs(e.getValue() - media) > umbral)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());

        return new OutlierResult(outliers, false);
    }

    // -------------------------------------------------------------------------
    // Helpers estadísticos
    // -------------------------------------------------------------------------

    private static double mean(double[] valores) {
        double suma = 0;
        for (double v : valores) {
            suma += v;
        }
        return suma / valores.length;
    }

    /**
     * Calcula la desviación estándar poblacional (divide entre N, no N-1).
     */
    private static double populationStddev(double[] valores, double media) {
        double varianza = 0;
        for (double v : valores) {
            double diff = v - media;
            varianza += diff * diff;
        }
        varianza /= valores.length;
        return Math.sqrt(varianza);
    }

    // -------------------------------------------------------------------------
    // Resultado
    // -------------------------------------------------------------------------

    /**
     * Resultado de la detección de outliers.
     *
     * @param outlierIds conjunto de IDs de analistas marcados como outliers
     * @param skipped    true si la detección fue omitida (N < 3)
     */
    public record OutlierResult(Set<String> outlierIds, boolean skipped) {}
}
