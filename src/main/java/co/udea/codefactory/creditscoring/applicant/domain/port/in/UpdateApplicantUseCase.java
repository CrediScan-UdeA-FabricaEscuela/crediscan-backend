package co.udea.codefactory.creditscoring.applicant.domain.port.in;

import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantCommand;
import co.udea.codefactory.creditscoring.applicant.application.dto.UpdateApplicantResult;

public interface UpdateApplicantUseCase {

    /**
     * Partially updates an applicant's allowed fields and records field-level audit entries.
     *
     * @param command the update command with only changed fields non-null
     * @return result containing the updated Applicant and list of changed field names
     * @throws co.udea.codefactory.creditscoring.applicant.domain.exception.ImmutableFieldException
     *         if identification or birthDate is present in the command
     * @throws co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException
     *         if no applicant exists with the given id
     */
    UpdateApplicantResult update(UpdateApplicantCommand command);
}
