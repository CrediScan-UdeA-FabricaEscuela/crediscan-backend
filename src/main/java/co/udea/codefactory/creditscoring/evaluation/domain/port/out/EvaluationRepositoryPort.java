package co.udea.codefactory.creditscoring.evaluation.domain.port.out;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.evaluation.domain.model.Evaluation;
import co.udea.codefactory.creditscoring.evaluation.domain.model.RiskLevel;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchCriteria;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationSearchItem;
import co.udea.codefactory.creditscoring.evaluation.domain.model.search.EvaluationStats;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

/**
 * Puerto de salida para la persistencia de evaluaciones crediticias.
 */
public interface EvaluationRepositoryPort {

    /** Persiste o actualiza una evaluación y retorna la entidad guardada. */
    Evaluation save(Evaluation evaluation);

    /** Busca una evaluación por su identificador único. */
    Optional<Evaluation> findById(UUID id);

    /**
     * Verifica si existe una evaluación para el solicitante evaluada después de {@code since}.
     * Se usa para verificar el cooldown antes de iniciar una nueva evaluación.
     */
    boolean existsByApplicantIdAndEvaluatedAtAfter(UUID applicantId, OffsetDateTime since);

    /** Retorna todas las evaluaciones ordenados por fecha descendente. */
    List<Evaluation> findAll();

    /**
     * Cuenta la última evaluación por solicitante agrupada por nivel de riesgo,
     * considerando solo evaluaciones dentro del rango de fechas indicado.
     *
     * @param desde inicio del rango (inclusive)
     * @param hasta fin del rango (inclusive)
     * @return mapa de nivel → conteo de solicitantes
     */
    Map<RiskLevel, Long> countLatestByRiskLevel(OffsetDateTime desde, OffsetDateTime hasta);

    /**
     * Retorna una página con la última evaluación por solicitante dentro del rango,
     * filtrada por nivel de riesgo. Incluye el nombre del solicitante via JOIN.
     *
     * @param nivel    nivel de riesgo a filtrar (null para todos los niveles)
     * @param desde    inicio del rango
     * @param hasta    fin del rango
     * @param pageRequest parámetros de paginación
     * @return resultado paginado de proyecciones con datos del solicitante
     */
    PagedResult<LatestEvaluationProjection> findLatestPerApplicantInRange(
            RiskLevel nivel, OffsetDateTime desde, OffsetDateTime hasta, PageRequest pageRequest);

    /**
     * Retorna todas las evaluaciones de un solicitante ordenadas por fecha descendente,
     * con desempate por id descendente.
     *
     * @param applicantId identificador del solicitante
     * @return lista ordenada (puede ser vacía si el solicitante no tiene evaluaciones)
     */
    List<Evaluation> findByApplicantIdOrderByEvaluatedAtDesc(UUID applicantId);

    /**
     * Busca evaluaciones aplicando criterios avanzados de filtrado con paginación.
     *
     * @param criteria criterios de búsqueda (fechas requeridas + filtros opcionales)
     * @param page     parámetros de paginación
     * @return página de items de evaluación con JOIN a applicant y credit_decision
     */
    PagedResult<EvaluationSearchItem> search(EvaluationSearchCriteria criteria, PageRequest page);

    /**
     * Calcula estadísticas agregadas (total, promedio, distribución por nivel)
     * aplicando los mismos filtros que {@link #search}.
     *
     * @param criteria criterios de búsqueda
     * @return estadísticas del conjunto filtrado
     */
    EvaluationStats stats(EvaluationSearchCriteria criteria);

    /**
     * Cuenta el número de evaluaciones que coinciden con los criterios dados.
     * Se usa para la validación del límite de filas antes de generar un PDF.
     *
     * @param criteria criterios de búsqueda
     * @return total de evaluaciones que cumplen los criterios
     */
    long countByCriteria(EvaluationSearchCriteria criteria);
}
