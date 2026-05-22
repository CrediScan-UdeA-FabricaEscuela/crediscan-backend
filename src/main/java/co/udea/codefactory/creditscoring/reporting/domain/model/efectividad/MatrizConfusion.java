package co.udea.codefactory.creditscoring.reporting.domain.model.efectividad;

import java.util.List;

/**
 * Matriz de confusión completa (6 niveles de riesgo × 4 decisiones = 24 celdas).
 * <p>
 * Siempre contiene las 24 combinaciones posibles: las celdas sin datos tienen {@code count=0}.
 * REJECTED se agrupa bajo VERY_HIGH según la decisión bloqueada #5.
 * </p>
 *
 * @param celdas lista plana de 24 celdas (gap-fill garantizado)
 */
public record MatrizConfusion(List<CeldaMatriz> celdas) {}
