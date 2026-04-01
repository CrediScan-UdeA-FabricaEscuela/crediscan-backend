package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface JpaApplicantEditAuditRepository extends JpaRepository<ApplicantEditAuditJpaEntity, UUID> {
}
