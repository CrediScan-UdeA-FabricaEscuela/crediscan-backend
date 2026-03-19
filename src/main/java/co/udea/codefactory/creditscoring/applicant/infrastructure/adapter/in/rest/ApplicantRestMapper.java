package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.applicant.application.dto.RegisterApplicantCommand;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.ApplicantResponse;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantRequest;
import co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.in.rest.dto.RegisterApplicantResponse;

@Component
public class ApplicantRestMapper {

    static final String SUCCESS_MESSAGE = "Solicitante registrado exitosamente";

    public RegisterApplicantCommand toCommand(RegisterApplicantRequest request) {
        return new RegisterApplicantCommand(
                request.name(),
                request.identification(),
                request.birthDate(),
                request.employmentType(),
                request.monthlyIncome(),
                request.workExperienceMonths());
    }

    public RegisterApplicantResponse toResponse(Applicant applicant) {
        ApplicantResponse applicantResponse = new ApplicantResponse(
                applicant.id(),
                applicant.name(),
                applicant.identification(),
                applicant.birthDate(),
                applicant.employmentType().apiValue(),
                applicant.monthlyIncome(),
                applicant.workExperienceMonths());
        return new RegisterApplicantResponse(applicant.id(), SUCCESS_MESSAGE, applicantResponse);
    }
}
