package co.udea.codefactory.creditscoring.applicant.domain.port.in;

import co.udea.codefactory.creditscoring.applicant.application.dto.RegisterApplicantCommand;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;

public interface RegisterApplicantUseCase {

    Applicant register(RegisterApplicantCommand command);
}
