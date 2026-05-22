package co.udea.codefactory.creditscoring.reporting.infrastructure.adapter.out.persistence.analistas;

import java.time.Instant;

/**
 * Proyección para la query de timestamps por evaluación (HU-017).
 * Se usa para calcular el tiempo de decisión en horas hábiles (en Java, no en SQL).
 * <p>
 * Usa {@link Instant} porque las native queries de Spring Data JPA proyectan
 * columnas {@code TIMESTAMP WITH TIME ZONE} como {@code Instant}; la conversión
 * a {@code OffsetDateTime} se realiza en el adaptador.
 * </p>
 */
public interface AnalistaTimestampsProjection {
    /** Identificador del analista. */
    String getEvaluatedBy();

    /** Timestamp de la evaluación (UTC). */
    Instant getEvaluatedAt();

    /** Timestamp de la decisión crediticia (UTC). */
    Instant getDecidedAt();
}
