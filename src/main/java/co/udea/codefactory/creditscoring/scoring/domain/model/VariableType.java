package co.udea.codefactory.creditscoring.scoring.domain.model;

/** Tipo de dato de una variable de scoring. */
public enum VariableType {

    /** Variable numérica con rangos de valoración (ej: antigüedad laboral). */
    NUMERIC,

    /** Variable categórica con puntajes directos por valor (ej: tipo de empleo). */
    CATEGORICAL;

    /** Retorna el VariableType que corresponde al valor de cadena almacenado en BD. */
    public static VariableType fromValor(String valor) {
        for (VariableType t : values()) {
            if (t.name().equalsIgnoreCase(valor)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Tipo de variable no reconocido: " + valor);
    }
}
