package co.udea.codefactory.creditscoring.applicant.application.service;

import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.SearchApplicantUseCase;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.IdentificationCryptoPort;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

@Service
public class SearchApplicantService implements SearchApplicantUseCase {

    private final ApplicantRepositoryPort applicantRepositoryPort;
    private final IdentificationCryptoPort identificationCryptoPort;

    public SearchApplicantService(
            ApplicantRepositoryPort applicantRepositoryPort,
            IdentificationCryptoPort identificationCryptoPort) {
        this.applicantRepositoryPort = applicantRepositoryPort;
        this.identificationCryptoPort = identificationCryptoPort;
    }

    @Override
    public PagedResult<ApplicantSummary> search(String criteria, PageRequest pageRequest) {
        if (criteria == null || criteria.isBlank()) {
            return applicantRepositoryPort.findAll(pageRequest);
        }
        String identificationHash = identificationCryptoPort.hash(criteria.trim());
        String nameCriteria = "%" + criteria.trim().toLowerCase() + "%";
        return applicantRepositoryPort.search(identificationHash, nameCriteria, pageRequest);
    }
}
