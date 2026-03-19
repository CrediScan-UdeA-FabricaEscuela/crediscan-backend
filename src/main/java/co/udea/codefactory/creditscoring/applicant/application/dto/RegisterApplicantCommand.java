package co.udea.codefactory.creditscoring.applicant.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterApplicantCommand(
        String name,
        String identification,
        LocalDate birthDate,
        String employmentType,
        BigDecimal monthlyIncome,
        Integer workExperienceMonths) {
}
