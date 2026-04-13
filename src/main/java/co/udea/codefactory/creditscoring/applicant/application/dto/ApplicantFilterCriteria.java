package co.udea.codefactory.creditscoring.applicant.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Criterios de filtrado para el listado paginado de solicitantes.
 * Todos los campos son opcionales y se combinan con lógica AND.
 * Los campos de ordenamiento (sortField, sortDirection) son independientes de los filtros.
 */
public record ApplicantFilterCriteria(
        String q,
        BigDecimal incomeMin,
        BigDecimal incomeMax,
        String employmentType,
        Integer experienceMin,
        Integer experienceMax,
        LocalDate registrationDateFrom,
        LocalDate registrationDateTo,
        String sortField,
        String sortDirection) {

    /**
     * Retorna true cuando ningún criterio de filtrado está activo.
     * Los campos de ordenamiento no se consideran filtros.
     */
    public boolean isEmpty() {
        return (q == null || q.isBlank())
                && incomeMin == null
                && incomeMax == null
                && (employmentType == null || employmentType.isBlank())
                && experienceMin == null
                && experienceMax == null
                && registrationDateFrom == null
                && registrationDateTo == null;
    }
}
