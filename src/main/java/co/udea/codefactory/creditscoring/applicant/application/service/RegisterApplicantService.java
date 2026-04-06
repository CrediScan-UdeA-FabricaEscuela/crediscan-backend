package co.udea.codefactory.creditscoring.applicant.application.service;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.udea.codefactory.creditscoring.applicant.application.dto.RegisterApplicantCommand;
import co.udea.codefactory.creditscoring.applicant.domain.exception.ApplicantValidationException;
import co.udea.codefactory.creditscoring.applicant.domain.exception.DuplicateApplicantException;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.applicant.domain.port.in.RegisterApplicantUseCase;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRegistrationMetricsPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.IdentificationCryptoPort;

@Service
public class RegisterApplicantService implements RegisterApplicantUseCase {

            private static final String DUPLICATE_IDENTIFICATION_MESSAGE =
                "El solicitante con esa identificación ya está registrado en el sistema";

    private final ApplicantRepositoryPort applicantRepositoryPort;
    private final IdentificationCryptoPort identificationCryptoPort;
    private final ApplicantRegistrationMetricsPort metricsPort;
    private final Clock clock;

    @Autowired
    public RegisterApplicantService(
            ApplicantRepositoryPort applicantRepositoryPort,
            IdentificationCryptoPort identificationCryptoPort,
            ApplicantRegistrationMetricsPort metricsPort) {
        this(applicantRepositoryPort, identificationCryptoPort, metricsPort, Clock.systemUTC());
    }

    RegisterApplicantService(
            ApplicantRepositoryPort applicantRepositoryPort,
            IdentificationCryptoPort identificationCryptoPort,
            ApplicantRegistrationMetricsPort metricsPort,
            Clock clock) {
        this.applicantRepositoryPort = applicantRepositoryPort;
        this.identificationCryptoPort = identificationCryptoPort;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public Applicant register(RegisterApplicantCommand command) {
        try {
            EmploymentType employmentType = EmploymentType.fromApiValue(command.employmentType());
            Applicant applicant = Applicant.registerNew(
                    command.name(),
                    command.identification(),
                    command.birthDate(),
                    employmentType,
                    command.monthlyIncome(),
                    command.workExperienceMonths(),
                    null,
                    command.address(),
                    command.email(),
                    clock);

            String identificationHash = identificationCryptoPort.hash(applicant.identification());
            if (applicantRepositoryPort.existsByIdentificationHash(identificationHash)) {
                throw new DuplicateApplicantException(DUPLICATE_IDENTIFICATION_MESSAGE);
            }

            String encryptedIdentification = identificationCryptoPort.encrypt(applicant.identification());
            Applicant savedApplicant = applicantRepositoryPort.save(applicant, encryptedIdentification, identificationHash);
            metricsPort.recordSuccess();
            return savedApplicant;
        } catch (ApplicantValidationException | DuplicateApplicantException ex) {
            metricsPort.recordFailure(ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
