package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest;

import java.util.UUID;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.application.dto.RegisterApplicantCommand;
import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantCommand;
import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantResult;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.ApplicantResponse;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.ApplicantSearchResponse;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantRequest;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantResponse;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.UpdateApplicantRequest;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.UpdateApplicantResponse;

@Component
public class ApplicantRestMapper {

    static final String REGISTER_SUCCESS_MESSAGE = "Solicitante registrado exitosamente";
    static final String UPDATE_SUCCESS_MESSAGE = "Solicitante actualizado exitosamente";

    public RegisterApplicantCommand toCommand(RegisterApplicantRequest request) {
        return new RegisterApplicantCommand(
                request.name(),
                request.identification(),
                request.birthDate(),
                request.employmentType(),
                request.monthlyIncome(),
                request.workExperienceMonths(),
                request.address(),
                request.email());
    }

    public RegisterApplicantResponse toResponse(Applicant applicant) {
        ApplicantResponse applicantResponse = toApplicantResponse(applicant);
        return new RegisterApplicantResponse(applicant.id(), REGISTER_SUCCESS_MESSAGE, applicantResponse);
    }

    public UpdateApplicantCommand toUpdateCommand(UUID id, UpdateApplicantRequest request, String actor) {
        return new UpdateApplicantCommand(
                id,
                actor,
                request.name(),
                request.identification(),
                request.birthDate(),
                request.employmentType(),
                request.monthlyIncome(),
                request.workExperienceMonths(),
                request.phone(),
                request.address(),
                request.email());
    }

    public UpdateApplicantResponse toUpdateResponse(UpdateApplicantResult result) {
        return new UpdateApplicantResponse(
                result.applicant().id(),
                UPDATE_SUCCESS_MESSAGE,
                result.changedFields(),
                toApplicantResponse(result.applicant()));
    }

    public ApplicantSearchResponse toSearchResponse(ApplicantSummary summary) {
        return new ApplicantSearchResponse(
                summary.id(),
                summary.name(),
                summary.identification(),
                summary.birthDate(),
                summary.employmentType(),
                summary.monthlyIncome(),
                summary.workExperienceMonths(),
                summary.phone(),
                summary.address(),
                summary.email());
    }

    private ApplicantResponse toApplicantResponse(Applicant applicant) {
        return new ApplicantResponse(
                applicant.id(),
                applicant.name(),
                applicant.identification(),
                applicant.birthDate(),
                applicant.employmentType().apiValue(),
                applicant.monthlyIncome(),
                applicant.workExperienceMonths(),
                applicant.phone(),
                applicant.address(),
                applicant.email());
    }
}
