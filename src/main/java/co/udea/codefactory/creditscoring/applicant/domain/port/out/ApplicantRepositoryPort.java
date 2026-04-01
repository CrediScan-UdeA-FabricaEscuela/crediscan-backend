package co.udea.codefactory.creditscoring.applicant.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;

public interface ApplicantRepositoryPort {

    boolean existsByIdentificationHash(String identificationHash);

    Applicant save(Applicant applicant, String encryptedIdentification, String identificationHash);

    Optional<Applicant> findById(UUID id);

    Page<ApplicantSummary> search(String identificationHash, String nameCriteria, Pageable pageable);

    Page<ApplicantSummary> findAll(Pageable pageable);

    Applicant update(Applicant applicant);
}
