package co.udea.codefactory.creditscoring.applicant.domain.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;

public interface SearchApplicantUseCase {

    /**
     * Search applicants by identification (exact match via HMAC hash) OR name (ILIKE).
     * If criteria is null or blank, returns all applicants paginated.
     *
     * @param criteria the search query (identification number or partial name); nullable
     * @param pageable pagination and sort parameters
     * @return paginated search results
     */
    Page<ApplicantSummary> search(String criteria, Pageable pageable);
}
