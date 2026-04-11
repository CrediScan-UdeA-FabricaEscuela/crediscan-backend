package co.udea.codefactory.creditscoring.applicant.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

public interface ApplicantRepositoryPort {

    boolean existsByIdentificationHash(String identificationHash);

    Applicant save(Applicant applicant, String encryptedIdentification, String identificationHash);

    Optional<Applicant> findById(UUID id);

    PagedResult<ApplicantSummary> search(String identificationHash, String nameCriteria, PageRequest pageRequest);

    PagedResult<ApplicantSummary> findAll(PageRequest pageRequest);

    Applicant update(Applicant applicant);
}
