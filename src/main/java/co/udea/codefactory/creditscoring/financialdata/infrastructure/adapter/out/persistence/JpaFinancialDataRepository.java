package co.udea.codefactory.creditscoring.financialdata.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface JpaFinancialDataRepository extends JpaRepository<FinancialDataJpaEntity, UUID> {

    Optional<FinancialDataJpaEntity> findFirstByApplicantIdOrderByVersionDesc(UUID applicantId);

    Optional<FinancialDataJpaEntity> findByApplicantIdAndVersion(UUID applicantId, int version);
}
