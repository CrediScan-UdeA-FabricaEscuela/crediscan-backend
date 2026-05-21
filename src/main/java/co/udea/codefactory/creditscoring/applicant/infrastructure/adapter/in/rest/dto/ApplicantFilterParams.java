package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Agrupa los parámetros de búsqueda/filtrado de solicitantes para evitar listas largas de parámetros
 * en los métodos del controlador (Sonar S107).
 */
public record ApplicantFilterParams(
        String q,
        BigDecimal incomeMin,
        BigDecimal incomeMax,
        String employmentType,
        Integer experienceMin,
        Integer experienceMax,
        LocalDate registrationDateFrom,
        LocalDate registrationDateTo) {
}
