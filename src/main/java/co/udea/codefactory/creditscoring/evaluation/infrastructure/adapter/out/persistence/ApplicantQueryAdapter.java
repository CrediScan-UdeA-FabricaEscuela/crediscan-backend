package co.udea.codefactory.creditscoring.evaluation.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.evaluation.domain.port.out.ApplicantQueryPort;

/**
 * Adaptador que implementa {@link ApplicantQueryPort} delegando al puerto público
 * del BC applicant ({@link ApplicantRepositoryPort}), preservando la separación
 * de bounded contexts sin acceder a JpaApplicantRepository directamente.
 */
@Component
public class ApplicantQueryAdapter implements ApplicantQueryPort {

    private final ApplicantRepositoryPort applicantRepositoryPort;

    public ApplicantQueryAdapter(ApplicantRepositoryPort applicantRepositoryPort) {
        this.applicantRepositoryPort = applicantRepositoryPort;
    }

    @Override
    public Optional<String> findNameById(UUID applicantId) {
        return applicantRepositoryPort.findById(applicantId)
                .map(applicant -> applicant.name());
    }
}
