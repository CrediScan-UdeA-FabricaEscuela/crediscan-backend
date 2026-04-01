package co.udea.codefactory.creditscoring.applicant.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Command for partial update of an applicant. All fields except applicantId and actor are nullable
 * (null means "do not change this field" — PATCH semantics).
 *
 * <p>If identification or birthDate are non-null, the service throws ImmutableFieldException.</p>
 */
public record UpdateApplicantCommand(
        UUID applicantId,
        String actor,
        String name,
        String identification,
        LocalDate birthDate,
        String employmentType,
        BigDecimal monthlyIncome,
        Integer workExperienceMonths,
        String phone) {
}
