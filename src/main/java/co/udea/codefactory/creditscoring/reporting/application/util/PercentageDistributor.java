package co.udea.codefactory.creditscoring.reporting.application.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Utilidad para distribuir porcentajes garantizando que la suma sea exactamente 100.00.
 * Implementa el método de mayor residuo (largest remainder method).
 */
public final class PercentageDistributor {

    private PercentageDistributor() {
        // utilidad estática — no instanciar
    }

    /**
     * Distribuye {@code total} unidades en porcentajes con {@code scale} decimales
     * usando el método de mayor residuo. Garantiza que la suma sea exactamente 100.00.
     *
     * @param counts lista de conteos por categoría
     * @param total  total de unidades (suma de counts)
     * @param scale  cantidad de decimales en el resultado (ej. 2 → 100.00)
     * @return lista de porcentajes con la misma longitud que {@code counts}
     */
    public static List<BigDecimal> largestRemainder(List<Long> counts, long total, int scale) {
        if (total == 0) {
            return counts.stream()
                    .map(c -> BigDecimal.ZERO.setScale(scale))
                    .toList();
        }

        // Factor = 10^scale (para scale=2 → 100)
        BigDecimal factor = BigDecimal.TEN.pow(scale);
        // Total en "unidades escaladas" = 100 * factor = 10000 para scale=2
        long totalUnits = BigDecimal.valueOf(100).multiply(factor).longValueExact();

        int n = counts.size();
        long[] integers = new long[n];
        BigDecimal[] remainders = new BigDecimal[n];
        long assigned = 0;

        for (int i = 0; i < n; i++) {
            BigDecimal raw = BigDecimal.valueOf(counts.get(i))
                    .multiply(BigDecimal.valueOf(totalUnits))
                    .divide(BigDecimal.valueOf(total), 10, RoundingMode.HALF_UP);
            integers[i] = raw.toBigInteger().longValueExact();
            remainders[i] = raw.subtract(BigDecimal.valueOf(integers[i]));
            assigned += integers[i];
        }

        long remaining = totalUnits - assigned;

        // Ordenar por residuo desc; tie-break por índice ascendente
        Integer[] order = IntStream.range(0, n).boxed().toArray(Integer[]::new);
        Arrays.sort(order, (a, b) -> {
            int cmp = remainders[b].compareTo(remainders[a]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });

        for (int k = 0; k < remaining; k++) {
            integers[order[k]]++;
        }

        // Convertir a porcentajes con `scale` decimales
        return Arrays.stream(integers)
                .mapToObj(v -> BigDecimal.valueOf(v).divide(factor, scale, RoundingMode.HALF_UP))
                .toList();
    }
}
