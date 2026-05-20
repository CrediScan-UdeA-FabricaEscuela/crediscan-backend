package co.udea.codefactory.creditscoring.reporting.domain.model;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Bin del histograma de puntajes.
 * Cubre el rango [binStart, binEnd) con binEnd == binStart + 10.
 * El último bin cubre [90, 100].
 */
public record HistogramBin(int binStart, int binEnd, long count) {

    public HistogramBin {
        if (binStart < 0 || binEnd > 100 || binEnd <= binStart) {
            throw new IllegalArgumentException(
                    "binStart debe ser >= 0, binEnd <= 100 y binEnd > binStart. " +
                    "Recibido: [" + binStart + ", " + binEnd + ")");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count debe ser >= 0");
        }
    }

    /**
     * Genera los 10 bins vacíos que cubren [0,10), [10,20), ..., [90,100].
     */
    public static List<HistogramBin> empty10Bins() {
        return IntStream.range(0, 10)
                .mapToObj(i -> new HistogramBin(i * 10, (i + 1) * 10, 0L))
                .toList();
    }
}
