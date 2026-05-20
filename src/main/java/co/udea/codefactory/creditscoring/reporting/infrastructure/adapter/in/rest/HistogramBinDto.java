package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.in.rest;

import co.udea.codefactory.creditscoring.reporting.domain.model.HistogramBin;

/**
 * DTO para cada bin del histograma de scores.
 */
public record HistogramBinDto(
        int binInicio,
        int binFin,
        long cantidad
) {
    public static HistogramBinDto from(HistogramBin b) {
        return new HistogramBinDto(b.binStart(), b.binEnd(), b.count());
    }
}
