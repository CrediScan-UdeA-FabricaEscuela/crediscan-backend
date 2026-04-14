package co.udea.codefactory.creditscoring.scoringmodel.domain.model;

/** Estado del ciclo de vida de una versión del modelo de scoring. */
public enum ModelStatus {

    /** Borrador: puede ser editado, no es válido para evaluaciones. */
    DRAFT,

    /** Activo: en uso para evaluaciones, de solo lectura. */
    ACTIVE,

    /** Inactivo: reemplazado por una versión posterior, de solo lectura. */
    INACTIVE;

    /** Retorna el ModelStatus a partir del valor almacenado en BD (case-insensitive). */
    public static ModelStatus fromValor(String valor) {
        for (ModelStatus s : values()) {
            if (s.name().equalsIgnoreCase(valor)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Estado de modelo no reconocido: " + valor);
    }
}
