package co.udea.codefactory.creditscoring.applicant.application.dto;

import java.util.List;

import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;

/**
 * Result returned by UpdateApplicantUseCase.
 * Contains the updated Applicant and the list of field names that were changed (for audit response).
 */
public record UpdateApplicantResult(
        Applicant applicant,
        List<String> changedFields) {
}
