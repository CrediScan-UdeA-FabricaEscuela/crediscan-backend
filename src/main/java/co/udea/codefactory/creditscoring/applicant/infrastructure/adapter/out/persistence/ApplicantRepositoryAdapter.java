package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.persistence;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import co.udea.codefactory.creditscoring.applicant.application.dto.ApplicantSummary;
import co.udea.codefactory.creditscoring.applicant.domain.model.Applicant;
import co.udea.codefactory.creditscoring.applicant.domain.model.EmploymentType;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.applicant.domain.port.out.IdentificationCryptoPort;

@Component
public class ApplicantRepositoryAdapter implements ApplicantRepositoryPort {

    private final JpaApplicantRepository jpaRepository;
    private final IdentificationCryptoPort identificationCryptoPort;
    private final Clock clock;

    @Autowired
    public ApplicantRepositoryAdapter(
            JpaApplicantRepository jpaRepository,
            IdentificationCryptoPort identificationCryptoPort) {
        this(jpaRepository, identificationCryptoPort, Clock.systemUTC());
    }

    ApplicantRepositoryAdapter(
            JpaApplicantRepository jpaRepository,
            IdentificationCryptoPort identificationCryptoPort,
            Clock clock) {
        this.jpaRepository = jpaRepository;
        this.identificationCryptoPort = identificationCryptoPort;
        this.clock = clock;
    }

    @Override
    public boolean existsByIdentificationHash(String identificationHash) {
        return jpaRepository.existsByIdentificationHash(identificationHash);
    }

    @Override
    public Applicant save(Applicant applicant, String encryptedIdentification, String identificationHash) {
        ApplicantJpaEntity entity = new ApplicantJpaEntity();
        entity.setId(applicant.id());
        entity.setName(applicant.name());
        entity.setIdentificationEncrypted(encryptedIdentification);
        entity.setIdentificationHash(identificationHash);
        entity.setBirthDate(applicant.birthDate());
        entity.setEmploymentType(applicant.employmentType().apiValue());
        entity.setMonthlyIncome(applicant.monthlyIncome());
        entity.setWorkExperienceMonths(applicant.workExperienceMonths());
        entity.setPhone(applicant.phone());
        entity.setAddress(applicant.address());
        entity.setEmail(applicant.email());
        entity.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setCreatedBy(currentUsername());
        jpaRepository.save(entity);
        return applicant;
    }

    @Override
    public Optional<Applicant> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<ApplicantSummary> search(String identificationHash, String nameCriteria, Pageable pageable) {
        return jpaRepository.searchByHashOrName(identificationHash, nameCriteria, pageable)
                .map(this::toSummary);
    }

    @Override
    public Page<ApplicantSummary> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(this::toSummary);
    }

    @Override
    public Applicant update(Applicant applicant) {
        ApplicantJpaEntity entity = jpaRepository.findById(applicant.id())
                .orElseThrow(() -> new IllegalStateException("Applicant not found during update: " + applicant.id()));
        entity.setName(applicant.name());
        entity.setEmploymentType(applicant.employmentType().apiValue());
        entity.setMonthlyIncome(applicant.monthlyIncome());
        entity.setWorkExperienceMonths(applicant.workExperienceMonths());
        entity.setPhone(applicant.phone());
        entity.setAddress(applicant.address());
        entity.setEmail(applicant.email());
        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedBy(currentUsername());
        jpaRepository.save(entity);
        return applicant;
    }

    private Applicant toDomain(ApplicantJpaEntity entity) {
        String plainIdentification = identificationCryptoPort.decrypt(entity.getIdentificationEncrypted());
        return Applicant.rehydrate(
                entity.getId(),
                entity.getName(),
                plainIdentification,
                entity.getBirthDate(),
                EmploymentType.fromApiValue(entity.getEmploymentType()),
                entity.getMonthlyIncome(),
                entity.getWorkExperienceMonths(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getEmail(),
                clock);
    }

    private ApplicantSummary toSummary(ApplicantJpaEntity entity) {
        String plainIdentification = identificationCryptoPort.decrypt(entity.getIdentificationEncrypted());
        return new ApplicantSummary(
                entity.getId(),
                entity.getName(),
                plainIdentification,
                entity.getBirthDate(),
                entity.getEmploymentType(),
                entity.getMonthlyIncome(),
                entity.getWorkExperienceMonths(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getEmail());
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "system";
    }
}
