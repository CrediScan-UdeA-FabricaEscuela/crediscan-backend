package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.persistence;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantFilterCriteria;

/**
 * Utilidad para construir Specifications dinámicas de filtrado sobre {@link ApplicantJpaEntity}.
 * Sigue el mismo patrón establecido en {@code AuditLogAdapter.buildSpecification()}.
 * Todos los predicados se combinan con lógica AND. Criterios nulos o vacíos no agregan predicado.
 */
class ApplicantSpecifications {

    private ApplicantSpecifications() {}

    /**
     * Construye la Specification a partir de los criterios de filtrado y el hash de identificación.
     * Cuando todos los criterios son nulos, retorna una conjunción (sin filtro = todos los registros).
     *
     * @param criteria          criterios de filtrado (nunca null)
     * @param identificationHash hash HMAC de la identificación, o null si q está vacío
     */
    static Specification<ApplicantJpaEntity> build(ApplicantFilterCriteria criteria, String identificationHash) {
        return (root, query, cb) -> {  // NOSONAR — query es parámetro mandatorio de Specification.toPredicate (JPA)
            List<Predicate> predicados = new ArrayList<>();

            agregarFiltroBusquedaLibre(predicados, criteria, identificationHash, root, cb);
            agregarFiltroIngresoMinimo(predicados, criteria, root, cb);
            agregarFiltroIngresoMaximo(predicados, criteria, root, cb);
            agregarFiltroTipoEmpleo(predicados, criteria, root, cb);
            agregarFiltroExperienciaMinima(predicados, criteria, root, cb);
            agregarFiltroExperienciaMaxima(predicados, criteria, root, cb);
            agregarFiltroFechaRegistroDesde(predicados, criteria, root, cb);
            agregarFiltroFechaRegistroHasta(predicados, criteria, root, cb);

            // Sin predicados → retorna todos los registros (conjunción vacía)
            return predicados.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    /** Búsqueda libre: hash exacto de identificación OR nombre parcial (ILIKE). */
    private static void agregarFiltroBusquedaLibre(
            List<Predicate> predicados,
            ApplicantFilterCriteria criteria,
            String identificationHash,
            Root<ApplicantJpaEntity> root,
            CriteriaBuilder cb) {
        boolean tieneHash = identificationHash != null;
        boolean tieneQ = criteria.q() != null && !criteria.q().isBlank();
        if (!tieneHash && !tieneQ) {
            return;
        }
        List<Predicate> predicadosBusqueda = new ArrayList<>();
        if (tieneHash) {
            predicadosBusqueda.add(cb.equal(root.get("identificationHash"), identificationHash));
        }
        if (tieneQ) {
            String patronNombre = "%" + criteria.q().trim().toLowerCase() + "%";
            predicadosBusqueda.add(cb.like(cb.lower(root.get("name")), patronNombre));
        }
        predicados.add(cb.or(predicadosBusqueda.toArray(new Predicate[0])));
    }

    /** Filtro por ingreso mensual mínimo. */
    private static void agregarFiltroIngresoMinimo(
            List<Predicate> predicados,
            ApplicantFilterCriteria criteria,
            Root<ApplicantJpaEntity> root,
            CriteriaBuilder cb) {
        if (criteria.incomeMin() != null) {
            predicados.add(cb.greaterThanOrEqualTo(root.get("monthlyIncome"), criteria.incomeMin()));
        }
    }

    /** Filtro por ingreso mensual máximo. */
    private static void agregarFiltroIngresoMaximo(
            List<Predicate> predicados,
            ApplicantFilterCriteria criteria,
            Root<ApplicantJpaEntity> root,
            CriteriaBuilder cb) {
        if (criteria.incomeMax() != null) {
            predicados.add(cb.lessThanOrEqualTo(root.get("monthlyIncome"), criteria.incomeMax()));
        }
    }

    /** Filtro por tipo de empleo (coincidencia exacta con el valor almacenado en DB). */
    private static void agregarFiltroTipoEmpleo(
            List<Predicate> predicados,
            ApplicantFilterCriteria criteria,
            Root<ApplicantJpaEntity> root,
            CriteriaBuilder cb) {
        if (criteria.employmentType() != null && !criteria.employmentType().isBlank()) {
            predicados.add(cb.equal(root.get("employmentType"), criteria.employmentType()));
        }
    }

    /** Filtro por antigüedad laboral mínima en meses. */
    private static void agregarFiltroExperienciaMinima(
            List<Predicate> predicados,
            ApplicantFilterCriteria criteria,
            Root<ApplicantJpaEntity> root,
            CriteriaBuilder cb) {
        if (criteria.experienceMin() != null) {
            predicados.add(cb.greaterThanOrEqualTo(root.get("workExperienceMonths"), criteria.experienceMin()));
        }
    }

    /** Filtro por antigüedad laboral máxima en meses. */
    private static void agregarFiltroExperienciaMaxima(
            List<Predicate> predicados,
            ApplicantFilterCriteria criteria,
            Root<ApplicantJpaEntity> root,
            CriteriaBuilder cb) {
        if (criteria.experienceMax() != null) {
            predicados.add(cb.lessThanOrEqualTo(root.get("workExperienceMonths"), criteria.experienceMax()));
        }
    }

    /** Filtro por fecha de registro desde (inicio del día en UTC). */
    private static void agregarFiltroFechaRegistroDesde(
            List<Predicate> predicados,
            ApplicantFilterCriteria criteria,
            Root<ApplicantJpaEntity> root,
            CriteriaBuilder cb) {
        if (criteria.registrationDateFrom() != null) {
            OffsetDateTime desde = criteria.registrationDateFrom().atStartOfDay().atOffset(ZoneOffset.UTC);
            predicados.add(cb.greaterThanOrEqualTo(root.get("createdAt"), desde));
        }
    }

    /** Filtro por fecha de registro hasta (fin del día en UTC). */
    private static void agregarFiltroFechaRegistroHasta(
            List<Predicate> predicados,
            ApplicantFilterCriteria criteria,
            Root<ApplicantJpaEntity> root,
            CriteriaBuilder cb) {
        if (criteria.registrationDateTo() != null) {
            OffsetDateTime hasta = criteria.registrationDateTo().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
            predicados.add(cb.lessThanOrEqualTo(root.get("createdAt"), hasta));
        }
    }
}
