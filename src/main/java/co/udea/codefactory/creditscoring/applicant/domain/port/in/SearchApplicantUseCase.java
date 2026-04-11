package co.udea.codefactory.creditscoring.applicant.domain.port.in;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

public interface SearchApplicantUseCase {

    /**
     * Search applicants by identification (exact match via HMAC hash) OR name (ILIKE).
     * If criteria is null or blank, returns all applicants paginated.
     *
     * @param criteria    the search query (identification number or partial name); nullable
     * @param pageRequest pagination parameters
     * @return paginated search results
     */
    PagedResult<ApplicantSummary> search(String criteria, PageRequest pageRequest);
}
