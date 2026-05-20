package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence;

/**
 * Proyección para la query del histograma de bins de score.
 */
public interface BinCountProjection {
    int getBinStart();
    long getCnt();
}
