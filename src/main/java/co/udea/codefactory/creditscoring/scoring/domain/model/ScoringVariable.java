package co.udea.codefactory.creditscoring.scoring.domain.model;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import co.udea.codefactory.creditscoring.scoring.domain.exception.ScoringVariableValidationException;

/**
 * Raíz del agregado para una variable de scoring crediticio.
 *
 * <p>Una variable numérica define rangos contiguos que asignan puntajes
 * según el valor observado. Una variable categórica asigna puntajes directos
 * a cada categoría posible del campo fuente.</p>
 */
public record ScoringVariable(
        UUID id,
        String nombre,
        String descripcion,
        VariableType tipo,
        BigDecimal peso,
        boolean activa,
        List<VariableRange> rangos,
        List<VariableCategory> categorias) {

    private static final BigDecimal PESO_MINIMO = new BigDecimal("0.01");
    private static final BigDecimal PESO_MAXIMO = BigDecimal.ONE;

    public ScoringVariable {
        if (nombre == null || nombre.isBlank()) {
            throw new ScoringVariableValidationException("El nombre de la variable es obligatorio");
        }
        if (tipo == null) {
            throw new ScoringVariableValidationException("El tipo de variable es obligatorio");
        }
        if (peso == null || peso.compareTo(PESO_MINIMO) < 0 || peso.compareTo(PESO_MAXIMO) > 0) {
            throw new ScoringVariableValidationException(
                    "El peso debe ser un valor entre 0.01 y 1.00 (recibido: " + peso + ")");
        }
        // Normalizar listas para evitar nulos
        rangos = rangos != null ? List.copyOf(rangos) : List.of();
        categorias = categorias != null ? List.copyOf(categorias) : List.of();

        if (tipo == VariableType.NUMERIC) {
            validarRangosNumericos(rangos);
        } else {
            validarCategorias(categorias);
        }
    }

    // -------------------------------------------------------------------------
    // Métodos de fábrica
    // -------------------------------------------------------------------------

    /** Crea una nueva variable generando un UUID aleatorio. */
    public static ScoringVariable crear(
            String nombre, String descripcion, VariableType tipo, BigDecimal peso,
            List<VariableRange> rangos, List<VariableCategory> categorias) {
        return new ScoringVariable(
                UUID.randomUUID(), nombre, descripcion, tipo, peso,
                true, rangos, categorias);
    }

    /** Reconstituye una variable existente desde el repositorio. */
    public static ScoringVariable rehydrate(
            UUID id, String nombre, String descripcion, VariableType tipo,
            BigDecimal peso, boolean activa,
            List<VariableRange> rangos, List<VariableCategory> categorias) {
        return new ScoringVariable(id, nombre, descripcion, tipo, peso, activa, rangos, categorias);
    }

    // -------------------------------------------------------------------------
    // Mutaciones (devuelven nuevas instancias — el record es inmutable)
    // -------------------------------------------------------------------------

    /** Retorna una copia de la variable con los nuevos rangos numéricos aplicados. */
    public ScoringVariable conRangos(List<VariableRange> nuevosRangos) {
        return new ScoringVariable(id, nombre, descripcion, tipo, peso, activa, nuevosRangos, List.of());
    }

    /** Retorna una copia de la variable con las nuevas categorías aplicadas. */
    public ScoringVariable conCategorias(List<VariableCategory> nuevasCategorias) {
        return new ScoringVariable(id, nombre, descripcion, tipo, peso, activa, List.of(), nuevasCategorias);
    }

    /** Retorna una copia con estado activo = true. */
    public ScoringVariable activar() {
        return new ScoringVariable(id, nombre, descripcion, tipo, peso, true, rangos, categorias);
    }

    /** Retorna una copia con estado activo = false. */
    public ScoringVariable desactivar() {
        return new ScoringVariable(id, nombre, descripcion, tipo, peso, false, rangos, categorias);
    }

    // -------------------------------------------------------------------------
    // Validaciones de dominio
    // -------------------------------------------------------------------------

    /**
     * Valida que los rangos de una variable numérica sean contiguos y sin solapamientos.
     * Según CA5 y RN5:
     * - Al menos un rango debe existir.
     * - Los rangos, ordenados por límite inferior, deben ser adyacentes:
     *   el límite superior de cada rango debe coincidir con el límite inferior del siguiente.
     * - El primer rango debe comenzar en 0 (RN5: "desde 0").
     */
    private static void validarRangosNumericos(List<VariableRange> rangos) {
        if (rangos.isEmpty()) {
            throw new ScoringVariableValidationException(
                    "Una variable numérica debe tener al menos un rango de valoración");
        }

        List<VariableRange> ordenados = rangos.stream()
                .sorted(Comparator.comparing(VariableRange::limiteInferior))
                .toList();

        // RN5: el primer rango debe comenzar en 0
        if (ordenados.get(0).limiteInferior().compareTo(BigDecimal.ZERO) != 0) {
            throw new ScoringVariableValidationException(
                    "El primer rango numérico debe comenzar en 0 (RN5: cubrir desde cero)");
        }

        // CA5: sin gaps ni solapamientos entre rangos consecutivos
        for (int i = 0; i < ordenados.size() - 1; i++) {
            BigDecimal limiteSuperiorActual = ordenados.get(i).limiteSuperior();
            BigDecimal limiteInferiorSiguiente = ordenados.get(i + 1).limiteInferior();
            if (limiteSuperiorActual.compareTo(limiteInferiorSiguiente) != 0) {
                throw new ScoringVariableValidationException(
                        String.format(
                                "Los rangos no son contiguos: el límite superior %s no coincide con "
                                        + "el límite inferior siguiente %s (CA5: sin gaps ni solapamientos)",
                                limiteSuperiorActual, limiteInferiorSiguiente));
            }
        }
    }

    /** Valida que una variable categórica tenga al menos una categoría (CA6). */
    private static void validarCategorias(List<VariableCategory> categorias) {
        if (categorias.isEmpty()) {
            throw new ScoringVariableValidationException(
                    "Una variable categórica debe tener al menos una categoría");
        }
    }
}
