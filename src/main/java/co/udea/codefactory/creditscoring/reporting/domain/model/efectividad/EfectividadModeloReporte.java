package co.udea.codefactory.creditscoring.reporting.domain.model.efectividad;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Reporte completo de efectividad del modelo de riesgo automático (HU-016).
 * <p>
 * Cuando {@code hasData=false}, los campos {@code matriz}, {@code indicadores} y
 * {@code overrides} pueden estar vacíos. Nunca se retorna HTTP 404 — siempre HTTP 200.
 * </p>
 *
 * @param matriz       matriz de confusión con gap-fill (24 celdas)
 * @param indicadores  tasas de concordancia y override
 * @param overrides    lista de casos de override individuales
 * @param hasData      true si existen evaluaciones con decisión en el rango
 * @param desde        inicio del rango consultado
 * @param hasta        fin del rango consultado
 * @param analistaId   analista filtrado (null = todos)
 */
public record EfectividadModeloReporte(
        MatrizConfusion matriz,
        IndicadoresEfectividad indicadores,
        List<CasoOverride> overrides,
        boolean hasData,
        OffsetDateTime desde,
        OffsetDateTime hasta,
        String analistaId
) {
    /**
     * Crea un reporte vacío (hasData=false) para cuando no hay evaluaciones
     * en el rango solicitado.
     */
    public static EfectividadModeloReporte empty(
            OffsetDateTime desde,
            OffsetDateTime hasta,
            String analistaId) {
        return new EfectividadModeloReporte(
                new MatrizConfusion(List.of()),
                null,
                List.of(),
                false,
                desde,
                hasta,
                analistaId);
    }
}
