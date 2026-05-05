package co.udea.codefactory.creditscoring.applicant.domain.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantFilterCriteria;
import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

// Puerto de entrada para el listado de solicitantes con filtros combinables y exportación CSV
public interface ListApplicantsUseCase {

    /**
     * Retorna una página de solicitantes que coinciden con los criterios dados.
     * Si todos los criterios están vacíos, retorna todos los solicitantes paginados.
     */
    PagedResult<ApplicantSummary> list(ApplicantFilterCriteria criteria, PageRequest pageRequest);

    /**
     * Retorna hasta MAX_EXPORT_SIZE solicitantes que coinciden con los criterios,
     * para ser exportados en formato CSV.
     */
    List<ApplicantSummary> export(ApplicantFilterCriteria criteria);

    /** Retorna un solicitante por su ID con la identificación descifrada, o vacío si no existe. */
    Optional<ApplicantSummary> findById(UUID id);
}
