package co.udea.codefactory.creditscoring.reporting.domain.model.analistas;

/**
 * Métricas de actividad de un analista en el período consultado (HU-017).
 *
 * <p>El campo {@code nombre} se incluye por especificación CA1-017. En este sistema,
 * la entidad {@code AppUser} no almacena nombre de pila ni apellido — solo {@code username}.
 * Por tanto {@code nombre} es siempre igual a {@code evaluatedBy} (fallback al username).</p>
 *
 * @param evaluatedBy             identificador del analista (username en {@code evaluation.evaluated_by})
 * @param nombre                  nombre para mostrar; fallback a {@code evaluatedBy} cuando no
 *                                hay fuente de nombre disponible (CA1-017)
 * @param totalEvaluaciones       total de evaluaciones con decisión
 * @param distribucion            distribución de las decisiones con porcentajes
 * @param tiempoMedioHorasHabiles tiempo promedio de decisión en horas hábiles
 * @param isOutlier               true si el tiempo medio es outlier respecto al equipo calificado
 */
public record ActividadAnalista(
        String evaluatedBy,
        String nombre,
        long totalEvaluaciones,
        DistribucionDecisiones distribucion,
        double tiempoMedioHorasHabiles,
        boolean isOutlier
) {}
