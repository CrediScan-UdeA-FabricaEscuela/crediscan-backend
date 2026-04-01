package co.udea.codefactory.creditscoring.applicant.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only projection used for search results.
 * Contains the decrypted (plaintext) identification for display.
 */
public record ApplicantSummary(
        UUID id,
        String name,
        String identification,
        LocalDate birthDate,
        String employmentType,
        BigDecimal monthlyIncome,
        Integer workExperienceMonths,
        String phone) {
}
