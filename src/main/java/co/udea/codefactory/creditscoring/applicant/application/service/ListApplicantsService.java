package co.udea.codefactory.creditscoring.applicant.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantFilterCriteria;
import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.ListApplicantsUseCase;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.IdentificationCryptoPort;
import co.udea.codefactory.creditscoring.shared.PageRequest;
import co.udea.codefactory.creditscoring.shared.PagedResult;

@Service
@Transactional(readOnly = true)
public class ListApplicantsService implements ListApplicantsUseCase {

    // Límite máximo de registros para exportación CSV, para proteger la memoria del servidor
    static final int MAX_EXPORT_SIZE = 10_000;

    private final ApplicantRepositoryPort applicantRepositoryPort;
    private final IdentificationCryptoPort identificationCryptoPort;

    public ListApplicantsService(
            ApplicantRepositoryPort applicantRepositoryPort,
            IdentificationCryptoPort identificationCryptoPort) {
        this.applicantRepositoryPort = applicantRepositoryPort;
        this.identificationCryptoPort = identificationCryptoPort;
    }

    @Override
    public PagedResult<ApplicantSummary> list(ApplicantFilterCriteria criteria, PageRequest pageRequest) {
        String hashIdentificacion = calcularHashSiHayCriterio(criteria.q());
        return applicantRepositoryPort.findByFilter(criteria, hashIdentificacion, pageRequest);
    }

    @Override
    public List<ApplicantSummary> export(ApplicantFilterCriteria criteria) {
        String hashIdentificacion = calcularHashSiHayCriterio(criteria.q());
        return applicantRepositoryPort.findAllByFilter(criteria, hashIdentificacion, MAX_EXPORT_SIZE);
    }

    /**
     * Calcula el hash HMAC de la identificación solo cuando el criterio de búsqueda es válido.
     * Retorna null cuando q es nulo o está en blanco, para que el adaptador omita el predicado.
     */
    private String calcularHashSiHayCriterio(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return identificationCryptoPort.hash(q.trim());
    }
}
