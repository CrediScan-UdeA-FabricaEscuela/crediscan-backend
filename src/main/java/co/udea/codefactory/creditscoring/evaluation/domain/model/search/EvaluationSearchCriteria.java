package co.udea.codefactory.creditscoring.evaluation.domain.model.search;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import co.udea.codefactory.creditscoring.evaluation.domain.exception.EvaluationValidationException;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;

/**
 * Criterios de búsqueda avanzada de evaluaciones crediticias.
 *
 * <p>Immutable record con compact constructor que valida todas las invariantes de negocio.
 * Las listas vacías de {@code niveles} y {@code decisiones} se normalizan a {@code null}
 * para simplificar la lógica SQL downstream.</p>
 */
public record EvaluationSearchCriteria(
        OffsetDateTime fechaDesde,
        OffsetDateTime fechaHasta,
        List<RiskLevel> niveles,
        BigDecimal puntajeMin,
        BigDecimal puntajeMax,
        List<DecisionFilterValue> decisiones,
        String analista
) {
    public EvaluationSearchCriteria {
        // Validación: fechas requeridas
        if (fechaDesde == null || fechaHasta == null) {
            throw new EvaluationValidationException("fecha_desde y fecha_hasta son requeridas");
        }
        // Validación: orden de fechas
        if (fechaHasta.isBefore(fechaDesde)) {
            throw new EvaluationValidationException("fecha_hasta debe ser >= fecha_desde");
        }
        // Validación: rango máximo de 365 días
        if (Duration.between(fechaDesde, fechaHasta).toDays() > 365) {
            throw new EvaluationValidationException("rango de fechas no puede exceder 365 días");
        }
        // Validación: rango de puntaje
        if (puntajeMin != null && puntajeMax != null && puntajeMin.compareTo(puntajeMax) > 0) {
            throw new EvaluationValidationException("puntaje_min debe ser <= puntaje_max");
        }
        // Normalización: listas vacías → null
        niveles    = (niveles    == null || niveles.isEmpty())    ? null : List.copyOf(niveles);
        decisiones = (decisiones == null || decisiones.isEmpty()) ? null : List.copyOf(decisiones);
    }
}
