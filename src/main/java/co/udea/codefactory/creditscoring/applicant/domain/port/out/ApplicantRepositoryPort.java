package co.udea.codefactory.creditscoring.applicant.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantFilterCriteria;
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

    /**
     * Retorna una página de solicitantes que satisfacen los criterios de filtrado.
     * El hash de identificación se precalcula en la capa de aplicación para mantener
     * la lógica de hashing fuera del adaptador.
     */
    PagedResult<ApplicantSummary> findByFilter(ApplicantFilterCriteria criteria,
            String identificationHash, PageRequest pageRequest);

    /**
     * Retorna hasta maxResults solicitantes que satisfacen los criterios,
     * sin paginación, para uso exclusivo en exportación CSV.
     */
    List<ApplicantSummary> findAllByFilter(ApplicantFilterCriteria criteria,
            String identificationHash, int maxResults);
}
