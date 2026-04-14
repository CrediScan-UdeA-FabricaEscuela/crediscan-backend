package co.udea.codefactory.creditscoring.scoringmodel.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoringmodel.domain.exception.ScoringModelValidationException;

/**
 * Raíz del agregado para una versión del modelo de scoring crediticio.
 *
 * <p>Una versión puede estar en estado DRAFT (editable), ACTIVE (en uso) o INACTIVE (histórico).
 * Solo una versión puede estar ACTIVE en cualquier momento (CA3).</p>
 */
public record ScoringModel(
        UUID id,
        String nombre,
        String descripcion,
        int version,
        ModelStatus estado,
        List<ModelVariable> variables,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActivacion) {

    private static final int MIN_VARIABLES_ACTIVAS = 3;

    public ScoringModel {
        if (nombre == null || nombre.isBlank()) {
            throw new ScoringModelValidationException("El nombre del modelo de scoring es obligatorio");
        }
        if (version < 1) {
            throw new ScoringModelValidationException("El número de versión debe ser mayor o igual a 1");
        }
        if (estado == null) {
            throw new ScoringModelValidationException("El estado del modelo es obligatorio");
        }
        variables = variables != null ? List.copyOf(variables) : List.of();
    }

    // -------------------------------------------------------------------------
    // Métodos de fábrica
    // -------------------------------------------------------------------------

    /** Crea una nueva versión en estado DRAFT con un UUID generado. */
    public static ScoringModel crear(
            String nombre, String descripcion, int version, List<ModelVariable> variables) {
        return new ScoringModel(
                UUID.randomUUID(), nombre, descripcion, version,
                ModelStatus.DRAFT, variables, OffsetDateTime.now(), null);
    }

    /** Reconstituye una versión existente desde el repositorio. */
    public static ScoringModel rehydrate(
            UUID id, String nombre, String descripcion, int version,
            ModelStatus estado, List<ModelVariable> variables,
            OffsetDateTime fechaCreacion, OffsetDateTime fechaActivacion) {
        return new ScoringModel(
                id, nombre, descripcion, version, estado, variables,
                fechaCreacion, fechaActivacion);
    }

    // -------------------------------------------------------------------------
    // Mutaciones (el record es inmutable — devuelven nuevas instancias)
    // -------------------------------------------------------------------------

    /**
     * Valida y retorna una nueva instancia en estado ACTIVE.
     * Aplica RN1 (suma de pesos = 1.00) y RN2 (mínimo 3 variables).
     * Solo un borrador puede ser activado (CA5).
     */
    public ScoringModel activar(OffsetDateTime ahora, List<ModelVariable> variablesConSnapshot) {
        if (estado != ModelStatus.DRAFT) {
            throw new ScoringModelValidationException(
                    "Solo un modelo en estado BORRADOR puede ser activado (estado actual: " + estado + ")");
        }
        if (variablesConSnapshot.size() < MIN_VARIABLES_ACTIVAS) {
            throw new ScoringModelValidationException(
                    "Se requieren al menos " + MIN_VARIABLES_ACTIVAS
                    + " variables para activar el modelo (RN2); el modelo tiene "
                    + variablesConSnapshot.size());
        }

        BigDecimal sumaPesos = variablesConSnapshot.stream()
                .map(ModelVariable::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumaPesos.compareTo(BigDecimal.ONE) != 0) {
            throw new ScoringModelValidationException(
                    "La suma de pesos de las variables debe ser exactamente 1.00 para activar (RN1); "
                    + "suma actual: " + sumaPesos);
        }

        return new ScoringModel(
                id, nombre, descripcion, version,
                ModelStatus.ACTIVE, variablesConSnapshot,
                fechaCreacion, ahora);
    }

    /** Retorna una nueva instancia en estado INACTIVE (reemplazada por otra versión). */
    public ScoringModel desactivar() {
        return new ScoringModel(
                id, nombre, descripcion, version,
                ModelStatus.INACTIVE, variables,
                fechaCreacion, fechaActivacion);
    }

    /** Indica si el modelo es editable (solo DRAFT lo es, CA5). */
    public boolean esEditable() {
        return estado == ModelStatus.DRAFT;
    }
}
