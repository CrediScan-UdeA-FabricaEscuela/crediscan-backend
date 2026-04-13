package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.persistence;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

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
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            // Búsqueda libre: hash exacto de identificación OR nombre parcial (ILIKE)
            boolean tieneHash = identificationHash != null;
            boolean tieneQ = criteria.q() != null && !criteria.q().isBlank();
            if (tieneHash || tieneQ) {
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

            // Filtro por rango de ingresos mensuales
            if (criteria.incomeMin() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("monthlyIncome"), criteria.incomeMin()));
            }
            if (criteria.incomeMax() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("monthlyIncome"), criteria.incomeMax()));
            }

            // Filtro por tipo de empleo (coincidencia exacta con el valor almacenado en DB)
            if (criteria.employmentType() != null && !criteria.employmentType().isBlank()) {
                predicados.add(cb.equal(root.get("employmentType"), criteria.employmentType()));
            }

            // Filtro por rango de antigüedad laboral en meses
            if (criteria.experienceMin() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("workExperienceMonths"), criteria.experienceMin()));
            }
            if (criteria.experienceMax() != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("workExperienceMonths"), criteria.experienceMax()));
            }

            // Filtro por rango de fecha de registro (inicio y fin del día en UTC)
            if (criteria.registrationDateFrom() != null) {
                OffsetDateTime desde = criteria.registrationDateFrom().atStartOfDay().atOffset(ZoneOffset.UTC);
                predicados.add(cb.greaterThanOrEqualTo(root.get("createdAt"), desde));
            }
            if (criteria.registrationDateTo() != null) {
                OffsetDateTime hasta = criteria.registrationDateTo().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
                predicados.add(cb.lessThanOrEqualTo(root.get("createdAt"), hasta));
            }

            // Sin predicados → retorna todos los registros (conjunción vacía)
            return predicados.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
